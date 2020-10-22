package faunadb.values

import scala.language.implicitConversions

object FieldPath {
  implicit def iToSegment(i: Int): FieldPath = FieldPathIdx(i)
  implicit def sToSegment(s: String): FieldPath = FieldPathField(s)
}

sealed abstract class FieldPath {
  def subValue(v: Result[Value]): Result[Value]

  def ++(other: FieldPath): FieldPath =
    this match {
      case FieldPathEmpty => other
      case p              => FieldPathNode(p, other)
    }
}

case object FieldPathEmpty extends FieldPath {
  override def toString = "/"
  def subValue(v: Result[Value]): Result[Value] = v
}

case class FieldPathField(field: String) extends FieldPath {
  override def toString = s"/$field"
  def subValue(v: Result[Value]): Result[Value] =
    v match {
      case VSuccess(ObjectV(values), pre) =>
        values.get(field) match {
          case Some(v) => VSuccess(v, pre ++ this)
          case None    => Result.NotFound(pre ++ this)
        }
      case VSuccess(v, pre) => Result.Unexpected(v, "Object", pre ++ this)
      case f: VFail => f
    }
}

case class FieldPathIdx(idx: Int) extends FieldPath {
  override def toString = s"/$idx"

  def subValue(v: Result[Value]): Result[Value] =
    v match {
      case VSuccess(ArrayV(elems), pre) =>
        if (elems.size > idx) {
          VSuccess(elems(idx), pre ++ this)
        } else {
          Result.NotFound(pre ++ this)
        }
      case VSuccess(v, pre) => Result.Unexpected(v, "Array", pre ++ this)
      case f: VFail => f
    }
}

case class FieldPathNode(l: FieldPath, r: FieldPath) extends FieldPath {
  override def toString =
    (l.toString, r.toString) match {
      case ("/", s) => s
      case (s, "/") => s
      case (l, r)   => s"$l$r"
    }

  def subValue(v: Result[Value]): Result[Value] =
    r.subValue(l.subValue(v))
}
