package com.faunadb.query

import com.fasterxml.jackson.annotation._

import scala.annotation.meta.{field, getter}
import scala.collection.JavaConverters._

sealed trait Response

case class Instance(ref: Ref, @(JsonProperty @field)("class") classRef: Ref, ts: Long, data: ObjectPrimitive) extends Response

case class Ref(@(JsonProperty @field)("@ref") ref: String) extends Retrievable with Response

case class Var(@(JsonProperty @field)("var") variable: String) extends Retrievable

sealed trait Primitive {
  def asObject = this match {
    case x: ObjectPrimitive => x
    case _ => null
  }

  def asArray = this match {
    case x: ArrayPrimitive => x
    case _ => null
  }

  def asString = this match {
    case x: StringPrimitive => x
    case _ => null
  }

  def asBoolean = this match {
    case x: BooleanPrimitive => x
    case _ => null
  }

  def asNumber = this match {
    case x: NumberPrimitive => x
    case _ => null
  }
}

object Primitives {
  implicit def stringToPrimitive(unwrapped: String) = StringPrimitive(unwrapped)
  implicit def longToPrimitive(unwrapped: Long) = NumberPrimitive(unwrapped)
  implicit def boolToPrimitive(unwrapped: Boolean) = BooleanPrimitive(unwrapped)
  implicit def arrayToPrimitive(unwrapped: Array[Primitive]) = ArrayPrimitive(unwrapped)
  implicit def mapToPrimitive(unwrapped: collection.Map[String, Primitive]) = ObjectPrimitive(unwrapped)
  implicit def doubleToPrimitive(unwrapped: Double) = DoublePrimitive(unwrapped)

  // Java interop
  def newObject(map: java.util.Map[String, Primitive]) = new ObjectPrimitive(map)
}

case object NullPrimitive extends Primitive {
  @(JsonValue @getter) val value = null
}

case class StringPrimitive(@(JsonValue @getter) value: String) extends Primitive
case class NumberPrimitive(@(JsonValue @getter) value: Long) extends Primitive
case class DoublePrimitive(@(JsonValue @getter) value: Double) extends Primitive
case class BooleanPrimitive(@(JsonValue @getter) value: Boolean) extends Primitive

case class ArrayPrimitive(@(JsonValue @getter) values: scala.Array[Primitive]) extends Primitive

case class ObjectPrimitive(@(JsonIgnore @field) values: collection.Map[String, Primitive]) extends Primitive {

  @JsonCreator
  def this(javaMap: java.util.Map[String, Primitive]) = this(javaMap.asScala)

  @JsonProperty("@object") def javaValues = values.asJava
}
