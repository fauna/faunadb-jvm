package com.faunadb.query

import com.fasterxml.jackson.annotation.{JsonIgnore, JsonValue, JsonProperty}
import scala.collection.JavaConverters._

import scala.annotation.meta.{getter, field}

case class Ref(@(JsonProperty @field)("@ref") ref: String) extends Retrievable

sealed trait Primitive
object Primitives {
  implicit def stringToPrimitive(unwrapped: String) = StringPrimitive(unwrapped)
  implicit def longToPrimitive(unwrapped: Long) = NumberPrimitive(unwrapped)
  implicit def boolToPrimitive(unwrapped: Boolean) = BooleanPrimitive(unwrapped)
  implicit def arrayToPrimitive(unwrapped: Array[Primitive]) = ArrayPrimitive(unwrapped)
  implicit def mapToPrimitive(unwrapped: Map[String, Primitive]) = ObjectPrimitive(unwrapped)
}

case class StringPrimitive(@(JsonValue @getter) value: String) extends Primitive
case class NumberPrimitive(@(JsonValue @getter) value: Long) extends Primitive
case class BooleanPrimitive(@(JsonValue @getter) value: Boolean) extends Primitive

case class ArrayPrimitive(@(JsonValue @getter) values: scala.Array[Primitive]) extends Primitive

case class ObjectPrimitive(@JsonIgnore values: Map[String, Primitive]) extends Primitive {
  @JsonProperty("object") def javaValues = values.asJava
}


