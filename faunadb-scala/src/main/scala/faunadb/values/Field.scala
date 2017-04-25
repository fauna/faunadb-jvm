package faunadb.values

import scala.collection.generic.CanBuildFrom
import scala.language.higherKinds

case class ValueReadException(errors: List[FieldError]) extends Exception {
  override def getMessage = errors map { _.message } mkString ", "
}

case class FieldError(error: String, path: FieldPath) {
  def message = s"Error at $path: $error"
  def prefixed(pre: FieldPath) = FieldError(error, pre ++ path)
}

object Field extends Field[Value] {
  val path = FieldPathEmpty
  def get(value: Value): Result[Value] = VSuccess(value, path)

  private abstract class TupleField[T] extends Field[T] {
    def path = FieldPathEmpty // FIXME: doesn't really apply.
  }

  def zip[A,B](f1: Field[A], f2: Field[B]): Field[(A,B)] =
    new TupleField[(A, B)] {
      def get(value: Value): Result[(A, B)] =
        (f1.get(value), f2.get(value)) match {
          case (VSuccess(a, _), VSuccess(b, _)) =>
            VSuccess((a, b), FieldPathEmpty)
          case (a, b) => Result.collectErrors(a, b)
        }
    }

  def zip[A,B,C](f1: Field[A], f2: Field[B], f3: Field[C]): Field[(A,B,C)] =
    new TupleField[(A, B, C)] {
      def get(value: Value): Result[(A, B, C)] =
        (f1.get(value), f2.get(value), f3.get(value)) match {
          case (VSuccess(a, _), VSuccess(b, _), VSuccess(c, _)) =>
            VSuccess((a, b, c), FieldPathEmpty)
          case (a, b, c) => Result.collectErrors(a, b, c)
        }
    }

  def zip[A,B,C,D](f1: Field[A], f2: Field[B], f3: Field[C], f4: Field[D]): Field[(A,B,C,D)] =
    new TupleField[(A, B, C, D)] {
      def get(value: Value): Result[(A, B, C, D)] =
        (f1.get(value), f2.get(value), f3.get(value), f4.get(value)) match {
          case (VSuccess(a, _), VSuccess(b, _), VSuccess(c, _), VSuccess(d, _)) =>
            VSuccess((a, b, c, d), FieldPathEmpty)
          case (a, b, c, d) => Result.collectErrors(a, b, c, d)
        }
    }

  def zip[A,B,C,D,E](f1: Field[A], f2: Field[B], f3: Field[C], f4: Field[D], f5: Field[E]): Field[(A,B,C,D,E)] =
    new TupleField[(A, B, C, D, E)] {
      def get(value: Value): Result[(A, B, C, D, E)] =
        (f1.get(value), f2.get(value), f3.get(value), f4.get(value), f5.get(value)) match {
          case (VSuccess(a, _), VSuccess(b, _), VSuccess(c, _), VSuccess(d, _), VSuccess(e, _)) =>
            VSuccess((a, b, c, d, e), FieldPathEmpty)
          case (a, b, c, d, e) =>
            Result.collectErrors(a, b, c, d, e)
        }
    }
}

sealed abstract class Field[T] {
  def path: FieldPath
  def get(value: Value): Result[T]

  def apply(p: FieldPath, ps: FieldPath*)(implicit ev: Field[T] =:= Field[Value]): Field[Value] =
    new SubField(ev(this), (ps foldLeft p) { _ ++ _ })

  def map[U](f: T => U): Field[U] = new MappedField(this, f)

  def to[U](implicit ev: Field[T] =:= Field[Value], dec: Decoder[U]): Field[U] =
    new TypedField(ev(this), dec)

  def collect[U, Col[_]](inner: Field[U])(implicit ev: Field[T] =:= Field[Value], cbf: CanBuildFrom[_, U, Col[U]]): Field[Col[U]] =
    new CollectionField(ev(this), cbf, inner.get _)
}

private[values] class SubField(parent: Field[Value], subpath: FieldPath) extends Field[Value] {
  def path = parent.path ++ subpath
  def get(value: Value) = subpath.subValue(parent.get(value))
}

private[values] class TypedField[T: Decoder](field: Field[Value], dec: Decoder[T]) extends Field[T] {
  def path = field.path
  def get(value: Value) =
    field.get(value) flatMap { dec.decode(_, path) }
}

private[values] class MappedField[T, U](field: Field[T], f: T => U) extends Field[U] {
  def path = field.path
  def get(value: Value) = field.get(value) map f
}

private[values] class CollectionField[T, Col](field: Field[Value], cbf: CanBuildFrom[_, T, Col], tr: Value => Result[T]) extends Field[Col] {
  def path = field.path
  def get(value: Value) =
    field.get(value) flatMap {
      case ArrayV(elems) =>
        val errs = List.newBuilder[FieldError]
        val rv = cbf()

        elems.zipWithIndex foreach {
          case (v, i) =>
            tr(v) match {
              case VSuccess(t, _) => rv += t
              case VFail(es) => errs ++= (es map { _.prefixed(path ++ FieldPathIdx(i)) })
            }
        }

        if (errs.result.nonEmpty) VFail(errs.result) else VSuccess(rv.result, path)

      case v => Result.Unexpected(v, "Array", path)
    }
}
