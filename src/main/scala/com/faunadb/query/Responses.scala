package com.faunadb.query

import java.util.{Map => JMap}

import com.fasterxml.jackson.annotation.JsonProperty

import scala.annotation.meta.field

sealed trait Response

case class SetResponse(data: Array[Ref], before: Option[Ref], after: Option[Ref]) extends Response
case class InstanceResponse(ref: Ref, @(JsonProperty @field)("class") classRef: Ref, ts: Long, data: JMap[String, Object]) extends Response
