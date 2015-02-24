package com.faunadb.query

import com.fasterxml.jackson.annotation.{JsonCreator, JsonProperty}
import java.util.{HashMap => JHashMap}

import scala.annotation.meta.field
import scala.collection.JavaConverters._

sealed trait Response

case class SetResponse(data: Array[Ref], before: Option[Ref], after: Option[Ref]) extends Response
case class InstanceResponse(ref: Ref, classRef: Ref, ts: Long, data: ObjectPrimitive) extends Response {
  @JsonCreator def this(ref: Ref, @(JsonProperty @field)("class") classRef: Ref, ts: Long, data: JHashMap[String, Primitive]) = this(ref, classRef, ts, ObjectPrimitive(data.asScala))
}
