package faunadb.types

import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.node.NullNode
import faunadb.response._

import scala.annotation.meta.getter

trait Value {
  def asStringOpt: Option[String] = None
  def asString = asStringOpt.get

  def asBooleanOpt: Option[Boolean] = None
  def asBoolean = asBooleanOpt.get

  def asNumberOpt: Option[Long] = None
  def asNumber = asNumberOpt.get

  def asDoubleOpt: Option[Double] = None
  def asDouble = asDoubleOpt.get

  def asArrayOpt: Option[Array[Value]] = None
  def asArray = asArrayOpt.get

  def asObjectOpt: Option[collection.Map[String, Value]] = None
  def asObject = asObjectOpt.get

  def asRefOpt: Option[Ref] = None
  def asRef = asRefOpt.get

  def asPageOpt: Option[Page] = None
  def asPage = asPageOpt.get

  def asInstanceOpt: Option[Instance] = None
  def asInstance = asInstanceOpt.get

  def asKeyOpt: Option[Key] = None
  def asKey = asKeyOpt.get

  def asDatabaseOpt: Option[Database] = None
  def asDatabase = asDatabaseOpt.get

  def asClassOpt: Option[faunadb.response.Class] = None
  def asClass = asClassOpt.get

  def asIndexOpt: Option[Index] = None
  def asIndex = asIndexOpt.get

  def asEventOpt: Option[Event] = None
  def asEvent = asEventOpt.get

  def asSetOpt: Option[faunadb.response.Set] = None
  def asSet = asSetOpt.get

  def asTsOpt: Option[Ts] = None
  def asTs = asTsOpt.get

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
