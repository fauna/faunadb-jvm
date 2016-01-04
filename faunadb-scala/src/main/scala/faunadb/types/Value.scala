package faunadb.types

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZonedDateTime, Instant}

import com.fasterxml.jackson.annotation.{JsonProperty, JsonCreator, JsonIgnore, JsonValue}
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.NullNode
import faunadb.response._

import scala.annotation.meta.{param, field, getter}

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
  *   value.asObject("ref").asRef // Ref("some/ref")
  *   value.asObject("data").asObject("someKey").asString // "string1"
  * }}}
  *
  * @define none [[scala.None]]
  */
trait Value {
  /**
    * Coerces the node into a string.
    *
    * @return [[scala.Some$]] if the coercion is possible, $none if not.
    */
  def asStringOpt: Option[String] = None
  def asString = asStringOpt.get

  /**
    * Coerces the node into a boolean
    *
    * @return [[scala.Some$]] if the coercion is possible, $none if not.
    */
  def asBooleanOpt: Option[Boolean] = None
  def asBoolean = asBooleanOpt.get

  /**
    * Coerces the node into a long.
    *
    * @return [[scala.Some$]] if the coercion is possible, $none if not.
    */
  def asNumberOpt: Option[Long] = None
  def asNumber = asNumberOpt.get

  /**
    * Coerces the node into a double.
    *
    * @return [[scala.Some$]] if the coercion is possible, $none if not.
    */
  def asDoubleOpt: Option[Double] = None
  def asDouble = asDoubleOpt.get

  /**
    * Coerces the node into an array.
    *
    * @return [[scala.Some$]] if the coercion is possible, $none if not.
    */
  def asArrayOpt: Option[Array[Value]] = None
  def asArray = asArrayOpt.get

  /**
    * Coerces the node into a map.
    *
    * @return [[scala.Some$]] if the coercion is possible, $none if not.
    */
  def asObjectOpt: Option[collection.Map[String, Value]] = None
  def asObject = asObjectOpt.get

  /**
    * Coerces the node into a [[Ref]].
    *
    * @return [[scala.Some$]] if the coercion is possible, $none if not.
    */
  def asRefOpt: Option[Ref] = None
  def asRef = asRefOpt.get

  /**
    * Coerces the node into a [[faunadb.response.Page]].
    *
    * @return [[scala.Some$]] if the coercion is possible, $none if not.
    */
  def asPageOpt: Option[Page] = None
  def asPage = asPageOpt.get

  /**
    * Coerces the node into an [[faunadb.response.Instance]].
    *
    * @return [[scala.Some$]] if the coercion is possible, $none if not.
    */
  def asInstanceOpt: Option[Instance] = None
  def asInstance = asInstanceOpt.get

  /**
    * Coerces the node into a [[faunadb.response.Key]].
    *
    * @return [[scala.Some$]] if the coercion is possible, $none if not.
    */
  def asKeyOpt: Option[Key] = None
  def asKey = asKeyOpt.get

  /**
    * Coerces the node into a [[faunadb.response.Token]]
    *
    * @return [[scala.Some$]] if the coercion is possible, $none if not.
    */
  def asTokenOpt: Option[Token] = None
  def asToken = asTokenOpt.get

  /**
    * Coerces the node into a [[faunadb.response.Database]]
    *
    * @return [[scala.Some$]] if the coercion is possible, $none if not.
    */
  def asDatabaseOpt: Option[Database] = None
  def asDatabase = asDatabaseOpt.get

  /**
    * Coerces the node into a [[faunadb.response.Class]]
    *
    * @return [[scala.Some$]] if the coercion is possible, $none if not.
    */
  def asClassOpt: Option[faunadb.response.Class] = None
  def asClass = asClassOpt.get

  /**
    * Coerces the node into an [[faunadb.response.Index]]
    *
    * @return [[scala.Some$]] if the coercion is possible, $none if not.
    */
  def asIndexOpt: Option[Index] = None
  def asIndex = asIndexOpt.get

  /**
    * Coerces the node into an [[faunadb.response.Event]].
    *
    * @return [[scala.Some$]] if the coercion is possible, $none if not.
    */
  def asEventOpt: Option[Event] = None
  def asEvent = asEventOpt.get

  /**
    * Coerces the node into a [[faunadb.response.Set]].
    *
    * @return [[scala.Some$]] if the coercion is possible, $none if not.
    */
  def asSetOpt: Option[faunadb.response.Set] = None
  def asSet = asSetOpt.get

  /**
    * Coerces the node into a [[faunadb.types.Ts]].
    *
    * @return [[scala.Some$]] if the coercion is possible, $none if not.
    */
  def asTsOpt: Option[Ts] = None
  def asTs = asTsOpt.get

  /**
    * Coerces the node into a [[faunadb.types.Date]].
    *
    * @return [[scala.Some$]] if the coercion is possible, $none if not.
    */
  def asDateOpt: Option[Date] = None
  def asDate = asDateOpt.get

  /**
   * Accesses the value of the specified field if this is an object node.
   */
  def apply(key: String): Value = asObject(key)

  /**
   * Accesses the value of the specified element if this is an array node.
   */
  def apply(index: Int): Value = asArray(index)

  /**
   * Accesses the value of the specified field if this is an object node.
   */
  def get(key: String): Option[Value] = asObjectOpt.flatMap(_.get(key))

  /**
   * Accesses the value of the specified element if this is an array node.
   */
  def get(index: Int): Option[Value] = asArrayOpt.flatMap(_.lift(index))

}

case class RawV(@(JsonValue @getter) value: JsonNode) extends Value

case class StringV(@(JsonValue @getter) value: String) extends Value {
  override def asStringOpt: Option[String] = Some(value)
}

case class NumberV(@(JsonValue @getter) value: Long) extends Value {
  override def asNumberOpt: Option[Long] = Some(value)
}

case class DoubleV(@(JsonValue @getter) value: Double) extends Value {
  override def asDoubleOpt: Option[Double] = Some(value)
}

case class BooleanV(@(JsonValue @getter) value: Boolean) extends Value {
  override def asBooleanOpt: Option[Boolean] = Some(value)
}

object ObjectV {
  val empty = new ObjectV(scala.collection.Map.empty[String, Value])
  def apply(pairs: (String, Value)*) = new ObjectV(pairs.toMap)
}

case class ObjectV(@(JsonValue @getter) values: collection.Map[String, Value]) extends Value {
  override def asObjectOpt: Option[collection.Map[String, Value]] = Some(values)
}

object ArrayV {
  val empty = new ArrayV(Array[Value]())

  def apply(items: Value*) = {
    new ArrayV(Array(items: _*))
  }
}

case class ArrayV(@(JsonValue @getter) values: scala.Array[Value]) extends Value {
  override def asArrayOpt: Option[Array[Value]] = Some(values)
}

case object NullV extends Value {
  @(JsonValue @getter) val value = NullNode.instance
}

object Ts {
  def apply(value: String) = new Ts(value)
}

case class Ts(@(JsonIgnore @param @field @getter) value: Instant) extends Value {
  @JsonCreator
  def this(@JsonProperty("@ts") value: String) = this(ZonedDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant)

  @JsonProperty("@ts")
  val strValue = value.toString

  override def asTsOpt = Some(this)
}

object Date {
  def apply(value: String) = new Date(value)
}

case class Date(@(JsonIgnore @param @field @getter) value: LocalDate) extends Value {
  @JsonCreator
  def this(@JsonProperty("@date") value: String) = this(LocalDate.parse(value))

  @JsonProperty("@date")
  val strValue = value.toString

  override def asDateOpt = Some(this)
}
