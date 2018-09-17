package faunadb.values

import java.nio.ByteBuffer
import java.time.{ Instant, LocalDate }
import java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME
import java.util.Base64;
import com.fasterxml.jackson.annotation._
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.node.NullNode
import faunadb.jackson._
import scala.annotation.meta.{ field, getter, param }

/**
  * A FaunaDB value.
  *
  * '''Reference''': [[https://fauna.com/documentation/queries#values FaunaDB Values]]
  *
  * ===Overview===
  *
  * Value is an algebraic data type that represents the value of
  * possible FaunaDB query responses. While it is possible to extract
  * data out of a Value object using pattern matching, the
  * [[faunadb.values.Field]] lets you create more complex and reusable
  * data extractors.
  *
  * {{{
  * // Simple, adhoc extraction:
  * value("data", "name").to[String].get
  *
  * // Using a reusable Field:
  * val NameField = Field("data", "name").to[String]
  * value(NameField).get
  * }}}
  *
  * Extraction can be composed:
  *
  * {{{
  * val refAndNameAndAge = for {
  *   ref <- value("ref").to[RefV]
  *   name <- value("data", "name").to[String]
  *   age <- value("data", "age").to[Int]
  * } yield (ref, name, age)
  *
  * refAndNameAndAge.get
  *
  * // or
  *
  * val RefAndNameAndAgeField = Field.zip(
  *   Field("ref").to[RefV],
  *   Field("data", "name").to[String],
  *   Field("data", "age").to[Int])
  *
  * value(RefAndNameAndAgeField).get
  * }}}
  *
  * If a value may be an array, or may contain an array, the array's
  * elements may be cast using [[collect]]:
  *
  * {{{
  * value("data", "tags").collect(Field.to[String]).get
  *
  * // or
  *
  * val TagsField = Field("data", "tags").collect(Field.to[String])
  * value(TagsField).get
  * }}}
  */
@JsonDeserialize(using=classOf[ValueDeserializer])
sealed trait Value {

  /** Extract a value with the provided field. */
  final def apply[T](field: Field[T]): Result[T] = field.get(this)

  /** Extract the sub-value at the specified path. */
  final def apply(p: FieldPath, ps: FieldPath*): Result[Value] = apply(Field(p, ps: _*))

  /** Cast the value to T, using a Decoder. */
  final def to[T: Decoder]: Result[T] = apply(Field.to[T])

  /** Extract the elements of an ArrayV using the provided field. */
  final def collect[T](field: Field[T]): Result[Seq[T]] = apply(Field.collect(field))

  /** Describe the type that this value will be representing */
  private[values] val vtype: String

}

/** Companion object to the Value trait. */
object Value {

  implicit def apply[T](value: T)(implicit encoder: Encoder[T]): Value = encoder.encode(value)

}

// Concrete Value types

/**
  * Base trait for all scalar values.
  *
  * Arrays, objects, and null are not considered scalar values.
  */
sealed abstract class ScalarValue(@JsonIgnore val vtype: String) extends Value

/** A String value. */
case class StringV(@JsonValue value: String) extends ScalarValue("String") {
  override def toString: String = s""""$value""""
}

/** A Long value. */
case class LongV(@(JsonValue @getter) value: Long) extends ScalarValue("Long") {
  override def toString: String = value.toString
}

/** A Double value. */
case class DoubleV(@(JsonValue @getter) value: Double) extends ScalarValue("Double") {
  override def toString: String = value.toString
}

/** A Boolean value. */
sealed abstract class BooleanV(@(JsonValue @getter) val value: Boolean) extends ScalarValue("Boolean") {
  // satisfy name-based extractor interface
  val isEmpty = false
  val get = value
  override def toString: String = value.toString
}
case object TrueV extends BooleanV(true)
case object FalseV extends BooleanV(false)

object BooleanV {
  def apply(b: Boolean) = if (b) TrueV else FalseV
  def unapply(b: BooleanV) = b
}

// Fauna special types

/** A Ref. */
case class RefV(@(JsonIgnore @getter) id: String,
                @(JsonIgnore @getter) clazz: Option[RefV] = None,
                @(JsonIgnore @getter) database: Option[RefV] = None) extends ScalarValue("Ref") {

  @JsonProperty("@ref")
  lazy val refValue: Any = RefID(id, clazz, database)

  @JsonInclude(JsonInclude.Include.NON_ABSENT)
  private case class RefID(
     @(JsonProperty @getter)("id") id: String,
     @(JsonProperty @getter)("class") clazz: Option[RefV],
     @(JsonProperty @getter)("database") database: Option[RefV])
}

object RefV {
  def apply(id: String, clazz: RefV): RefV = RefV(id, Option(clazz))
  def apply(id: String, clazz: RefV, database: RefV): RefV = RefV(id, Option(clazz), Option(database))
}

object Native {
  val Classes: RefV = RefV("classes", None, None)
  val Indexes: RefV = RefV("indexes", None, None)
  val Databases: RefV = RefV("databases", None, None)
  val Functions: RefV = RefV("functions", None, None)
  val Keys: RefV = RefV("keys", None, None)
}

/** A Set Ref. */
case class SetRefV(@JsonProperty("@set") parameters: Value) extends ScalarValue("SetRef")

/** A Timestamp value. */
case class TimeV(@(JsonIgnore @param @field @getter) toInstant: Instant) extends ScalarValue("Time") {

  @JsonProperty("@ts")
  val strValue = toInstant.toString
}
object TimeV {
  def apply(value: String): TimeV = TimeV(Instant.from(ISO_OFFSET_DATE_TIME.parse(value)))
}

/** A Date value. */
case class DateV(@(JsonIgnore @param @field @getter) localDate: LocalDate) extends ScalarValue("Date") {
  @JsonProperty("@date")
  val strValue = localDate.toString
}
object DateV {
  def apply(value: String): DateV = DateV(LocalDate.parse(value))
}

case class BytesV(@(JsonIgnore @param @field @getter) bytes: Array[Byte]) extends ScalarValue("Bytes") {
  @JsonProperty("@bytes")
  lazy val strValue = Base64.getUrlEncoder.encodeToString(bytes)

  override def equals(obj: Any): Boolean =
    obj match {
      case b: BytesV => ByteBuffer.wrap(bytes) == ByteBuffer.wrap(b.bytes)
      case _ => false
    }

  override def hashCode(): Int = bytes.hashCode()

  override def toString: String = bytes map { s => f"0x$s%02x" } mkString ("BytesV(", ", ", ")")
}
object BytesV {
  def apply(bytes: Int*): BytesV = BytesV(bytes map { _.toByte } toArray)
  def apply(value: String): BytesV = BytesV(Base64.getUrlDecoder.decode(value))
}

case class QueryV(@JsonProperty("@query") lambda: ObjectV) extends Value {
  @JsonIgnore val vtype: String = "Query"
}

// Container types and Null

/** An Object value. */
case class ObjectV(@(JsonValue @getter) fields: Map[String, Value]) extends Value {
  @JsonIgnore val vtype: String = "Object"
}
object ObjectV {
  val empty = ObjectV()
  def apply(fields: (String, Value)*) = new ObjectV(fields.toMap)
}

/** An Array. */
case class ArrayV(@(JsonValue @getter) elems: Vector[Value]) extends Value {
  @JsonIgnore val vtype: String = "Array"
}
object ArrayV {
  val empty = ArrayV()
  def apply(elems: Value*) = new ArrayV(Vector(elems: _*))
}

/** The Null value. */
sealed trait NullV extends Value {
  @JsonIgnore val vtype: String = "Null"
}
case object NullV extends NullV {
  @(JsonValue @getter) val value = NullNode.instance
}
