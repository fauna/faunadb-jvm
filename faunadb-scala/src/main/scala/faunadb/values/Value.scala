package faunadb.values

import com.fasterxml.jackson.annotation.{ JsonProperty, JsonCreator, JsonIgnore, JsonValue }
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.node.NullNode
import faunadb.jackson._
import java.time.format.DateTimeFormatter
import java.time.{ LocalDate, ZonedDateTime, Instant }
import scala.annotation.meta.{ param, field, getter }

/**
  * An abstract node in a FaunaDB response tree. Something conforming to this trait should be
  * coerced into a concrete type before data can be accessed.
  *
  * @example Consider the Value modeling the root of the following tree:
  * {{{
  * {
  *   "ref": {"@ref": "some/ref" },
  *   "data": { "someKey": "string1", "someKey2": 123 }
  * }
  * }}}
  *
  * The data in this tree can be accessed using:
  * {{{
  *   value("ref").as[Ref].get // Ref("some/ref")
  *   value("data", "someKey").as[String].get // "string1"
  * }}}
  * @define none [[scala.None]]
  */
@JsonDeserialize(using=classOf[ValueDeserializer])
sealed trait Value {

  /**
    * Extract a value with the provided field.
    */
  final def apply[T](field: Field[T]): Result[T] = field.get(this)

  /**
    * Extract the sub-value at the specified path.
    */
  final def apply(p: FieldPath, ps: FieldPath*): Result[Value] = apply(Field(p, ps: _*))

  /**
    * Cast the value to T, using a Decoder.
    */
  final def as[T: Decoder]: Result[T] = apply(Field.as[T])

  /**
    * Extract the elements of an ArrayV using the provided field.
    */
  final def collect[T](field: Field[T]): Result[Seq[T]] = apply(Field.collect(field))
}

// Concrete Value types

/**
  * Base trait for all scalar values.
  */
sealed trait ScalarValue extends Value

/**
  *  A String value.
  *
  * '''Reference''': [[https://faunadb.com/documentation/queries#values FaunaDB Values]]
  */
case class StringV(@(JsonValue @getter) value: String) extends ScalarValue

/**
  *  A Long value.
  *
  * '''Reference''': [[https://faunadb.com/documentation/queries#values FaunaDB Values]]
  */
case class LongV(@(JsonValue @getter) value: Long) extends ScalarValue

/**
  *  A Double value.
  *
  * '''Reference''': [[https://faunadb.com/documentation/queries#values FaunaDB Values]]
  */
case class DoubleV(@(JsonValue @getter) value: Double) extends ScalarValue

/**
  *  A Boolean value.
  *
  * '''Reference''': [[https://faunadb.com/documentation/queries#values FaunaDB Values]]
  */
object BooleanV {
  def apply(b: Boolean) = if (b) TrueV else FalseV
  def unapply(b: BooleanV) = b
}
sealed abstract class BooleanV(@(JsonValue @getter) val value: Boolean) extends ScalarValue {
  // satisfy name-based extractor interface
  val isEmpty = false
  val get = value
}
case object TrueV extends BooleanV(true)
case object FalseV extends BooleanV(false)

// Fauna special types

/**
  * A Ref.
  *
  * '''Reference''': [[https://faunadb.com/documentation/queries#values-special_types FaunaDB Special Types]]
  */
object Ref {
  def apply(clss: Ref, id: String): Ref = Ref(s"${clss.value}/$id")
}

case class Ref(@(JsonProperty @field @param)("@ref") value: String) extends ScalarValue

/**
  * A Set Ref.
  *
  * '''Reference''': [[https://faunadb.com/documentation/queries#values-special_types FaunaDB Special Types]]
  */
case class SetRef(@JsonProperty("@set") parameters: Value) extends ScalarValue

/**
  * A Timestamp.
  *
  * '''Reference''': [[https://faunadb.com/documentation/queries#values-special_types FaunaDB Special Types]]
  */
object Timestamp {
  def apply(value: String): Timestamp =
    Timestamp(ZonedDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant)
}

case class Timestamp(@(JsonIgnore @param @field @getter) instant: Instant) extends ScalarValue {
  @JsonProperty("@ts")
  val strValue = instant.toString
}

/**
  * A Date.
  *
  * '''Reference''': [[https://faunadb.com/documentation/queries#values-special_types FaunaDB Special Types]]
  */
object Date {
  def apply(value: String): Date = Date(LocalDate.parse(value))
}

case class Date(@(JsonIgnore @param @field @getter) localDate: LocalDate) extends ScalarValue {
  @JsonProperty("@date")
  val strValue = localDate.toString
}

// Container types and Null

/**
  * An Object.
  *
  * '''Reference''': [[https://faunadb.com/documentation/queries#values FaunaDB Values]]
  */
object ObjectV {
  val empty = ObjectV()
  def apply(fields: (String, Value)*) = new ObjectV(fields.toMap)
}

case class ObjectV(@(JsonValue @getter) fields: Map[String, Value]) extends Value

/**
  * An Array.
  *
  * '''Reference''': [[https://faunadb.com/documentation/queries#values FaunaDB Values]]
  */
object ArrayV {
  val empty = ArrayV()
  def apply(elems: Value*) = new ArrayV(Vector(elems: _*))
}

case class ArrayV(@(JsonValue @getter) elems: Vector[Value]) extends Value

/**
  * Null.
  *
  * '''Reference''': [[https://faunadb.com/documentation/queries#values FaunaDB Values]]
  */
sealed trait NullV extends Value
case object NullV extends NullV {
  @(JsonValue @getter) val value = NullNode.instance
}
