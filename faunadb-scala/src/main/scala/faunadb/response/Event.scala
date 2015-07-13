package faunadb.response

import com.fasterxml.jackson.annotation.JsonProperty
import faunadb.types.Ref

import scala.annotation.meta.field

case class Event(@(JsonProperty @field)("resource") resource: Ref,
                 @(JsonProperty @field)("action") action: String,
                 @(JsonProperty @field)("ts") ts: Long)
