package com.faunadb.client.query

import com.fasterxml.jackson.annotation._
import com.fasterxml.jackson.databind.annotation.JsonSerialize

import java.lang.{ Iterable => JIterable }
import scala.annotation.meta.{param, field, getter}
import scala.collection.JavaConverters._

sealed trait Expression
sealed trait Identifier extends Expression



sealed trait Path
case class ObjectPath(@(JsonValue @getter) field: String) extends Path
case class ArrayPath(@(JsonValue @getter) index: Int) extends Path

case class Let(@(JsonProperty @field)("let") vars: collection.Map[String, Expression], in: Expression) extends Expression
object Do {
  def create[A >: Expression](expressions: JIterable[A]) = new Do(expressions.asScala.map { _.asInstanceOf[Expression]})
}

case class Do(@(JsonIgnore @field @getter) expressions: Iterable[Expression]) extends Expression {
  @JsonCreator
  def this(@JsonProperty("do") expressions: JIterable[Expression]) = this(expressions.asScala)
  @JsonProperty("do")
  def javaExpressions = expressions.asJavaCollection
}

case class If(@(JsonProperty @field)("if") condition: Expression, then: Expression, `else`: Expression) extends Expression
case class Quote(quote: Expression) extends Expression
case class Fetch(@(JsonProperty @field)("fetch") path: Iterable[Path], from: Value) extends Expression {
  def this(path: JIterable[Path], from: Value) = this(path.asScala, from)
}

case class Lambda(@(JsonProperty @field)("lambda") argument: String, expr: Expression)
case class Map(@(JsonProperty @field)("map") lambda: Lambda, collection: Expression) extends Expression
case class Foreach(@(JsonProperty @field)("foreach") lambda: Lambda, collection: Expression) extends Expression



sealed trait Set extends Identifier

case class Match(@(JsonProperty @field)("match") term: Value, @(JsonProperty @field) index: Ref) extends Set {
  def this(term: String, index: Ref) = this(StringV(term), index)
}

object Union {
  def create[A >: Set](expressions: JIterable[A]) = new Union(expressions.asScala.map { _.asInstanceOf[Set] })
}
case class Union(@(JsonIgnore @field @getter) sets: Iterable[Set]) extends Set {
  @JsonCreator
  def this(@JsonProperty("union") sets: JIterable[Set]) = this(sets.asScala)
  @JsonProperty("union")
  def javaSets = sets.asJavaCollection
}

object Intersection {
  def create[A >: Set](sets: JIterable[A]) = new Intersection(sets.asScala.map { _.asInstanceOf[Set] })
}
case class Intersection(@(JsonIgnore @field @getter) sets: Iterable[Set]) extends Set {
  @JsonCreator
  def this(@JsonProperty("intersection") sets: JIterable[Set]) = this(sets.asScala)

  @JsonProperty("intersection")
  def javaSets = sets.asJavaCollection
}

object Difference {
  def create[A >: Set](sets: JIterable[A]) = new Difference(sets.asScala.map { _.asInstanceOf[Set] })
}
case class Difference(@(JsonIgnore @field @getter) sets: Iterable[Set]) extends Set {
  @JsonCreator
  def this(@JsonProperty("difference") sets: JIterable[Set]) = this(sets.asScala)

  @JsonProperty("difference")
  def javaSets = sets.asJavaCollection
}

case class Join(@(JsonProperty @field)("join") source: Set, @(JsonProperty @field)("with") target: String) extends Set

case class Get(@(JsonProperty @field)("get") resource: Identifier) extends Identifier

object Paginate {
  def create(resource: Identifier) = new Paginate(resource, None, None, None)
}

@JsonSerialize(using = classOf[PaginateSerializer])
case class Paginate(resource: Identifier,
                    ts: Option[Long] = None,
                    cursor: Option[Cursor] = None,
                    size: Option[Long] = None) extends Identifier {
  def withCursor(cursor: Cursor) = copy(cursor = Some(cursor))
  def withSize(size: Long) = copy(size = Some(size))
}

@JsonSerialize(using = classOf[EventsSerializer])
case class Events(resource: Identifier, cursor: Option[Cursor] = None, size: Option[Long] = None) extends Identifier

case class Create(@(JsonProperty @field)("create") ref: Identifier, @(JsonProperty @field)("params") obj: ObjectV = ObjectV.empty) extends Identifier
case class Replace(@(JsonProperty @field)("replace") ref: Identifier, @(JsonProperty @field)("params") obj: ObjectV) extends Identifier
case class Update(@(JsonProperty @field)("update") ref: Identifier, @(JsonProperty @field)("params") obj: ObjectV) extends Identifier
case class Delete(@(JsonProperty @field)("delete") ref: Identifier) extends Identifier


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

case class Ref(@(JsonProperty @field @param)("@ref") value: String) extends Value with Identifier {
  def this(parent: Ref, child: String) = this(parent.value + "/" + child)
}
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

case class ObjectV(@(JsonIgnore @field @getter) values: collection.Map[String, Value]) extends Value {
  @JsonCreator
  def this(javaMap: java.util.Map[String, Value]) = this(javaMap.asScala)

  @JsonProperty("@object") def javaValues = values.asJava
}
