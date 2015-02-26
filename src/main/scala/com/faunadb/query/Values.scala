package com.faunadb.query

import com.fasterxml.jackson.annotation._
import com.fasterxml.jackson.databind.annotation.JsonSerialize

import scala.annotation.meta.{param, field, getter}
import scala.collection.JavaConverters._

sealed trait Expression
sealed trait Identifier extends Expression



sealed trait Path
case class ObjectPath(@(JsonValue @getter) field: String) extends Path
case class ArrayPath(@(JsonValue @getter) index: Int) extends Path

case class Let(@(JsonProperty @field)("let") vars: collection.Map[String, Expression], in: Expression) extends Expression
case class Do(@(JsonProperty @field)("do") expressions: Array[Expression]) extends Expression
case class If(@(JsonProperty @field)("if") condition: Expression, then: Expression, `else`: Expression) extends Expression
case class Quote(quote: Expression) extends Expression
case class Fetch(@(JsonProperty @field)("fetch") path: Array[Path], from: Value) extends Expression

case class Lambda(@(JsonProperty @field)("lambda") argument: String, expr: Expression)
case class Map(@(JsonProperty @field)("map") lambda: Lambda, collection: Expression) extends Expression
case class Foreach(@(JsonProperty @field)("foreach") lambda: Lambda, collection: Expression) extends Expression



sealed trait Set extends Identifier

case class Match(@(JsonProperty @field)("match") term: String, @(JsonProperty @field) index: Ref) extends Set
case class Union(@(JsonProperty @field)("union") sets: Array[Set]) extends Set
case class Intersection(@(JsonProperty @field)("intersection") sets: Array[Set]) extends Set
case class Difference(@(JsonProperty @field)("difference") sets: Array[Set]) extends Set
case class Join(@(JsonProperty @field)("join") source: Set, @(JsonProperty @field)("with") target: String) extends Set

case class Get(@(JsonProperty @field)("get") resource: Identifier) extends Identifier

object Paginate {
  def create(resource: Identifier) = new Paginate(resource, None, None, None)
}

@JsonSerialize(using = classOf[PaginateSerializer])
case class Paginate(resource: Identifier,
                    ts: Option[Long] = None,
                    cursor: Option[Cursor] = None,
                    size: Option[Long] = None) extends Identifier

@JsonSerialize(using = classOf[EventsSerializer])
case class Events(resource: Identifier, cursor: Option[Cursor] = None, size: Option[Long] = None) extends Identifier

case class Create(@(JsonProperty @field)("create") ref: Ref, @(JsonProperty @field)("params") obj: ObjectV = ObjectV.empty) extends Identifier
case class Put(@(JsonProperty @field)("put") ref: Ref, @(JsonProperty @field)("params") obj: ObjectV) extends Identifier
case class Patch(@(JsonProperty @field)("patch") ref: Ref, @(JsonProperty @field)("params") obj: ObjectV) extends Identifier
case class Delete(@(JsonProperty @field)("delete") ref: Ref) extends Identifier

sealed trait Response

sealed trait Value extends Response with Expression {
  def asObject = this match {
    case x: ObjectV => x.values
    case _ => null
  }

  def asArray = this match {
    case x: ArrayV => x.values
    case _ => null
  }

  def asString = this match {
    case x: StringV => x.value
    case _ => null
  }

  def asBoolean = this match {
    case x: BooleanV => x.value
    case _ => null
  }

  def asNumber = this match {
    case x: NumberV => x.value
    case _ => null
  }

  def asDouble = this match {
    case x: DoubleV => x.value
    case _ => null
  }

  def asRef = this match {
    case x: Ref => x
    case _ => null
  }
}

case class Ref(@(JsonProperty @field @param)("@ref") value: String) extends Value with Identifier
case class Var(@(JsonProperty @field)("var") variable: String) extends Identifier

sealed trait Resource extends Response

case class Instance(@(JsonProperty)("ref") ref: Ref,
                    @(JsonProperty @field @param)("class") classRef: Ref,
                    @JsonProperty("ts") ts: Long,
                    @JsonProperty("data") data: ObjectV) extends Resource

case class Key(@JsonProperty("ref") ref: Ref,
               @(JsonProperty @field @param)("class") classRef: Ref,
               @JsonProperty("database") database: Ref,
               @JsonProperty("role") role: String,
               @JsonProperty("secret") secret: String,
               @(JsonProperty @field @param)("hashed_secret") hashedSecret: String,
               @JsonProperty("ts") ts: Long,
               @JsonProperty("data") data: ObjectV) extends Resource

case class Database(@JsonProperty("ref") ref: Ref,
                    @(JsonProperty @field @param)("class") classRef: Ref,
                    @JsonProperty("ts") ts: Long,
                    @JsonProperty("name") name: String) extends Resource

case class Class(@JsonProperty("ref") ref: Ref,
                 @(JsonProperty @field @param)("class") classRef: Ref,
                 @JsonProperty("ts") ts: Long,
                 @(JsonProperty @field @param)("history_days") historyDays: Long,
                 @JsonProperty("name") name: String) extends Resource
case class Index(@JsonProperty("ref") ref: Ref,
                 @(JsonProperty @field @param)("class") classRef: Ref,
                 @JsonProperty("ts") ts: Long,
                 @JsonProperty("unique") unique: Boolean,
                 @JsonProperty("active") active: Boolean,
                 @JsonProperty("name") name: String,
                 @JsonProperty("source") source: Ref,
                 @JsonProperty("path") path: String) extends Resource

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

  // Java interop
  def newObject(map: java.util.Map[String, Value]) = new ObjectV(map)
}

case object NullPrimitive extends Value {
  @(JsonValue @getter) val value = null
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

case class ObjectV(@(JsonIgnore @field) values: collection.Map[String, Value]) extends Value {
  @JsonCreator
  def this(javaMap: java.util.Map[String, Value]) = this(javaMap.asScala)

  @JsonProperty("@object") def javaValues = values.asJava
}
