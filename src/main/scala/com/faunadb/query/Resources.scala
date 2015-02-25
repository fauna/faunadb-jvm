package com.faunadb.query

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonSerialize

import scala.annotation.meta.field

trait Retrievable

sealed trait Resource extends Expression

case class Get(@(JsonProperty @field)("get") resource: Retrievable) extends Resource

object Paginate {
  def create(resource: Retrievable) = new Paginate(resource, None, None, None)
}

@JsonSerialize(using = classOf[PaginateSerializer])
case class Paginate(resource: Retrievable,
               ts: Option[Long] = None,
               cursor: Option[Cursor] = None,
               size: Option[Long] = None) extends Resource

@JsonSerialize(using = classOf[EventsSerializer])
case class Events(resource: Retrievable, cursor: Option[Cursor] = None, size: Option[Long] = None) extends Resource

case class Create(@(JsonProperty @field)("create") ref: Ref, @(JsonProperty @field)("params") obj: ObjectPrimitive = ObjectPrimitive.empty) extends Resource
case class Put(@(JsonProperty @field)("put") ref: Ref, @(JsonProperty @field)("params") obj: ObjectPrimitive) extends Resource
case class Patch(@(JsonProperty @field)("patch") ref: Ref, @(JsonProperty @field)("params") obj: ObjectPrimitive) extends Resource
case class Delete(@(JsonProperty @field)("delete") ref: Ref) extends Resource
