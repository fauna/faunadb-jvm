package com.faunadb.client.query

import java.lang.{Iterable => JIterable}

import com.fasterxml.jackson.annotation._
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.node.NullNode

import scala.annotation.meta.{field, getter, param}

sealed trait Expression
sealed trait Identifier extends Expression



object Path {
  implicit def stringToObjectPath(str: String) = ObjectPath(str)
  implicit def intToArrayPath(i: Int) = ArrayPath(i)
}

sealed trait Path
case class ObjectPath(@(JsonValue @getter) field: String) extends Path
case class ArrayPath(@(JsonValue @getter) index: Int) extends Path

case class Let(@(JsonProperty @field)("let") vars: collection.Map[String, Expression], in: Expression) extends Expression
case class Do(@(JsonProperty @field @getter)("do") expressions: Iterable[Expression]) extends Expression

case class If(@(JsonProperty @field)("if") condition: Expression, then: Expression, `else`: Expression) extends Expression
case class Quote(@(JsonProperty @field)("quote") quote: Expression) extends Expression
case class Select(@(JsonProperty @field)("select") path: Iterable[Path], from: Value) extends Expression

case class Lambda(@(JsonProperty @field)("lambda") argument: String, expr: Expression)
case class Map(@(JsonProperty @field)("map") lambda: Lambda, collection: Expression) extends Expression
case class Foreach(@(JsonProperty @field)("foreach") lambda: Lambda, collection: Expression) extends Expression



sealed trait Set extends Identifier

case class Match(@(JsonProperty @field)("match") term: Value, @(JsonProperty @field) index: Ref) extends Set {
  def this(term: String, index: Ref) = this(StringV(term), index)
}

case class Union(@(JsonProperty @field @getter)("union") sets: Iterable[Set]) extends Set

case class Intersection(@(JsonProperty @field @getter)("intersection") sets: Iterable[Set]) extends Set

case class Difference(@(JsonProperty @field @getter)("difference") sets: Iterable[Set]) extends Set

case class Join(@(JsonProperty @field)("join") source: Set, @(JsonProperty @field)("with") target: Lambda) extends Set

case class Get(@(JsonProperty @field)("get") resource: Identifier) extends Identifier

@JsonSerialize(using = classOf[PaginateSerializer])
case class Paginate(resource: Identifier,
                    ts: Option[Long] = None,
                    cursor: Option[Cursor] = None,
                    size: Option[Long] = None,
                    sources: Boolean = false,
                    events: Boolean = false) extends Identifier {
  def withCursor(cursor: Cursor) = copy(cursor = Some(cursor))
  def withSize(size: Long) = copy(size = Some(size))
}

case class Count(@(JsonProperty @field)("count") set: Set) extends Identifier

case class Create(@(JsonProperty @field)("create") ref: Identifier, @(JsonProperty @field)("params") obj: ObjectV = ObjectV.empty) extends Identifier
case class Replace(@(JsonProperty @field)("replace") ref: Identifier, @(JsonProperty @field)("params") obj: ObjectV) extends Identifier
case class Update(@(JsonProperty @field)("update") ref: Identifier, @(JsonProperty @field)("params") obj: ObjectV) extends Identifier
case class Delete(@(JsonProperty @field)("delete") ref: Identifier) extends Identifier


sealed trait Response

sealed trait Value extends Response with Expression {
  def asObject: scala.collection.Map[String, Value] = this match {
    case x: ObjectV => x.values
    case _ => null
  }

  def asArray: Array[Value] = this match {
    case x: ArrayV => x.values
    case _ => null
  }

  def asString: String = this match {
    case x: StringV => x.value
    case _ => null
  }

  def asBoolean: java.lang.Boolean = this match {
    case x: BooleanV => x.value
    case _ => null
  }

  def asNumber: java.lang.Long = this match {
    case x: NumberV => x.value
    case _ => null
  }

  def asDouble: java.lang.Double = this match {
    case x: DoubleV => x.value
    case _ => null
  }

  def asRef: Ref = this match {
    case x: Ref => x
    case _ => null
  }
}

case class Ref(@(JsonProperty @field @param)("@ref") value: String) extends Value with Identifier {
  def this(parent: Ref, child: String) = this(parent.value + "/" + child)
}
case class Var(@(JsonProperty @field)("var") variable: String) extends Value with Identifier

sealed trait Resource extends Response


object Values {
  implicit def stringToValue(unwrapped: String) = StringV(unwrapped)
  implicit def longToValue(unwrapped: Long) = NumberV(unwrapped)
  implicit def boolToValue(unwrapped: Boolean) = BooleanV(unwrapped)
  implicit def arrayToValue(unwrapped: Array[Value]) = ArrayV(unwrapped)
  implicit def mapToValue(unwrapped: collection.Map[String, Value]) = ObjectV(unwrapped)
  implicit def doubleToValue(unwrapped: Double) = DoubleV(unwrapped)

  implicit def pairToValuePair[T](p: (String, T))(implicit convert: T => Value) = {
    (p._1, convert(p._2))
  }
}

case object NullV extends Value {
  @(JsonValue @getter) val value = NullNode.instance;
}

case class StringV(@(JsonValue @getter) value: String) extends Value
case class NumberV(@(JsonValue @getter) value: Long) extends Value
case class DoubleV(@(JsonValue @getter) value: Double) extends Value
case class BooleanV(@(JsonValue @getter) value: Boolean) extends Value

object ArrayV {
  val empty = new ArrayV(Array[Value]())

  def apply(items: Value*) = {
    new ArrayV(Array(items: _*))
  }
}

case class ArrayV(@(JsonValue @getter) values: scala.Array[Value]) extends Value

object ObjectV {
  val empty = new ObjectV(scala.collection.Map.empty[String, Value])
  def apply(pairs: (String, Value)*) = new ObjectV(pairs.toMap)
}

case class ObjectV(@(JsonProperty @field @getter)("object") values: collection.Map[String, Value]) extends Value
