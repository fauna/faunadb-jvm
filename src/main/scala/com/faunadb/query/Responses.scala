package com.faunadb.query

import com.fasterxml.jackson.annotation.JsonProperty

import scala.annotation.meta.field
import java.util.Map

sealed trait Response

case class Element(value: Ref, sets: Array[Set])
case class SetResponse(data: Array[Element]) extends Response

case class InstanceResponse(ref: Ref, @(JsonProperty @field)("class") classRef: Ref, ts: Long, data: Map[String, Object]) extends Response
