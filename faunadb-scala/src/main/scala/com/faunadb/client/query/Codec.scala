package com.faunadb.client.query

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind._

private[query] class PaginateSerializer extends JsonSerializer[Paginate] {
  override def serialize(t: Paginate, jsonGenerator: JsonGenerator, serializerProvider: SerializerProvider): Unit = {
    jsonGenerator.writeStartObject()
    jsonGenerator.writeObjectField("paginate", t.resource)

    t.ts.foreach { tsNum =>
      jsonGenerator.writeNumberField("ts", tsNum)
    }

    t.cursor.foreach {
      case b: Before => jsonGenerator.writeObjectField("before", b.value)
      case a: After => jsonGenerator.writeObjectField("after", a.value)
    }

    t.size.foreach { sNum =>
      jsonGenerator.writeNumberField("size", sNum)
    }

    if (t.events) {
      jsonGenerator.writeBooleanField("events", t.events)
    }

    if (t.sources) {
      jsonGenerator.writeBooleanField("sources", t.sources)
    }

    jsonGenerator.writeEndObject()
  }
}

