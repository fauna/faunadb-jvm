package com.faunadb.client.jackson

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.{ObjectMapper, SerializerProvider, JsonSerializer}
import com.fasterxml.jackson.databind.module.SimpleModule
import com.faunadb.client.query.{ObjectV, FaunaDeserializerModifier}

class ExternalObjectSerializer extends JsonSerializer[ObjectV] {
  override def serialize(value: ObjectV, gen: JsonGenerator, serializers: SerializerProvider): Unit = {
    val json = gen.getCodec.asInstanceOf[ObjectMapper]
    json.writeValue(gen, value)
  }
}

class FaunaModule extends SimpleModule {
  setDeserializerModifier(new FaunaDeserializerModifier)
  addSerializer(classOf[ObjectV], new ExternalObjectSerializer)
}
