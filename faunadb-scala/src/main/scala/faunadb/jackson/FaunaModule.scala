package faunadb.jackson

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.{JsonSerializer, ObjectMapper, SerializerProvider}
import faunadb.query.ObjectV

class ExternalObjectSerializer extends JsonSerializer[ObjectV] {
  override def serialize(value: ObjectV, gen: JsonGenerator, serializers: SerializerProvider): Unit = {
    val json = gen.getCodec.asInstanceOf[ObjectMapper]
    json.writeValue(gen, value)
  }
}

class FaunaModule extends SimpleModule {
  addSerializer(classOf[ObjectV], new ExternalObjectSerializer)
}
