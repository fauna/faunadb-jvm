package com.faunadb.query

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.{SerializerProvider, JsonSerializer}

import scala.annotation.meta.field

trait Retrievable

sealed trait Resource extends Expression

case class Get(@(JsonProperty @field)("get") resource: Retrievable) extends Resource

@JsonSerialize(using = classOf[PaginateSerializer])
case class Paginate(resource: Retrievable,
               ts: Option[Long] = None,
               cursor: Option[Cursor] = None,
               size: Option[Long] = None) extends Resource

class PaginateSerializer extends JsonSerializer[Paginate] {
  override def serialize(t: Paginate, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider): Unit = {
    jsonGenerator.writeStartObject()
    jsonGenerator.writeObjectField("paginate", t.resource)

    t.ts.foreach { tsNum =>
      jsonGenerator.writeNumberField("ts", tsNum)
    }

    t.cursor.foreach {
      case b: Before => jsonGenerator.writeObjectField("before", b.ref)
      case a: After => jsonGenerator.writeObjectField("after", a.ref)
    }

    t.size.foreach { sNum =>
      jsonGenerator.writeNumberField("size", sNum)
    }

    jsonGenerator.writeEndObject()
  }
}

@JsonSerialize(using = classOf[EventsSerializer])
case class Events(resource: Retrievable, cursor: Option[Cursor] = None, size: Option[Long] = None) extends Resource

class EventsSerializer extends JsonSerializer[Events] {
  override def serialize(t: Events, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider): Unit = {
    jsonGenerator.writeStartObject()
    jsonGenerator.writeObjectField("events", t.resource)

    t.cursor.foreach {
      case b: Before => jsonGenerator.writeObjectField("before", b.ref)
      case a: After => jsonGenerator.writeObjectField("after", a.ref)
    }

    t.size.foreach { sNum =>
      jsonGenerator.writeNumberField("size", sNum)
    }

    jsonGenerator.writeEndObject()
  }
}

case class Create(@(JsonProperty @field)("create") ref: Ref, @(JsonProperty @field)("params") obj: ObjectPrimitive) extends Resource
case class Put(@(JsonProperty @field)("put") ref: Ref, @(JsonProperty @field)("params") obj: ObjectPrimitive) extends Resource
case class Patch(@(JsonProperty @field)("patch") ref: Ref, @(JsonProperty @field)("params") obj: ObjectPrimitive) extends Resource
case class Delete(@(JsonProperty @field)("delete") ref: Ref) extends Resource
