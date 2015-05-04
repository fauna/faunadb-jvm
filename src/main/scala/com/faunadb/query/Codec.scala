package com.faunadb.query

import com.fasterxml.jackson.core.{JsonParseException, JsonParser, JsonGenerator}
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.`type`.TypeFactory
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier
import java.util.HashMap
import com.fasterxml.jackson.core.JsonToken._

import com.fasterxml.jackson.databind.node.ObjectNode
import com.faunadb.query.Error.{UnknownError, ValidationFailed}
import scala.collection.JavaConversions._

class FaunaDeserializerModifier extends BeanDeserializerModifier {
  val setDeserializer = new SetDeserializer
  val primitiveDeserializer = new PrimitiveDeserializer
  val errorDeserializer = new ErrorDeserializer

  override def modifyDeserializer(config: DeserializationConfig, beanDesc: BeanDescription, deserializer: JsonDeserializer[_]): JsonDeserializer[_] = {
    if (beanDesc.getBeanClass == classOf[Set])
      setDeserializer
    else if (beanDesc.getBeanClass == classOf[Value])
      primitiveDeserializer
    else if (beanDesc.getBeanClass == classOf[Error]) {
      errorDeserializer
    } else
      deserializer
  }
}

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

class PrimitiveDeserializer extends JsonDeserializer[Value] {
  override def deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): Value = {
    val json = jsonParser.getCodec.asInstanceOf[ObjectMapper]
    jsonParser.getCurrentToken match {
      case VALUE_TRUE => BooleanV(true)
      case VALUE_FALSE => BooleanV(false)
      case VALUE_STRING => StringV(jsonParser.getValueAsString)
      case VALUE_NUMBER_INT => new NumberV(jsonParser.getValueAsLong())
      case VALUE_NUMBER_FLOAT => new DoubleV(jsonParser.getValueAsDouble())
      case VALUE_NULL => NullPrimitive
      case START_OBJECT =>
        val objectTree = json.readTree[ObjectNode](jsonParser)
        if (objectTree.has("@object")) {
          val objectValue = objectTree.get("@object").asInstanceOf[ObjectNode]
          new ObjectV(json.convertValue(objectValue, TypeFactory.defaultInstance().constructMapType(classOf[HashMap[_,_]], classOf[String], classOf[Value])).asInstanceOf[java.util.Map[String, Value]])
        } else if (objectTree.has("@ref")) {
          json.treeToValue(objectTree, classOf[Ref])
        } else {
          new ObjectV(json.convertValue(objectTree, TypeFactory.defaultInstance().constructMapType(classOf[HashMap[_,_]], classOf[String], classOf[Value])).asInstanceOf[java.util.Map[String, Value]])
        }
      case START_ARRAY =>
        ArrayV(json.readValue(jsonParser, TypeFactory.defaultInstance().constructArrayType(classOf[Value])).asInstanceOf[Array[Value]])
      case sym@_ =>
        throw new JsonParseException("Unable to deserialize Fauna primitive. Unexpected token: "+sym, jsonParser.getCurrentLocation)
    }
  }
}

object SetDeserializer {
  val SetClasses = collection.Map(
    "match" -> classOf[Match],
    "union" -> classOf[Union],
    "intersection" -> classOf[Intersection],
    "difference" -> classOf[Difference],
    "join" -> classOf[Join]
  )
}

class SetDeserializer extends JsonDeserializer[Set] {
  override def deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): Set = {
    val loc = jsonParser.getCurrentLocation
    val json = jsonParser.getCodec.asInstanceOf[ObjectMapper]
    val tree = json.readTree(jsonParser).asInstanceOf[ObjectNode]

    val fields = tree.fieldNames.filter(SetDeserializer.SetClasses.keySet.contains(_)).toSeq
    if (fields.length > 1) {
      throw new JsonParseException("Set object cannot contain multiple function names.", loc)
    }

    val clazz = SetDeserializer.SetClasses(fields.head)
    json.treeToValue(tree, clazz)
  }
}

class ErrorDeserializer extends JsonDeserializer[Error] {
  override def deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): Error = {
    val json = jsonParser.getCodec.asInstanceOf[ObjectMapper]
    val tree = json.readTree(jsonParser).asInstanceOf[ObjectNode]

    val code = tree.get("code").asText()
    code match {
      case Errors.ValidationFailed => json.treeToValue(tree, classOf[ValidationFailed])
      case _ => json.treeToValue(tree, classOf[UnknownError])
    }
  }
}
