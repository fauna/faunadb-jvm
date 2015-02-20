package com.faunadb.query

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.{JsonParseException, JsonParser}
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind._

import scala.annotation.meta.field
import scala.collection.JavaConversions._

trait SetDeserialization

sealed trait Set extends Retrievable

case class Match(@(JsonProperty @field)("match") term: String, @(JsonProperty @field) index: Ref) extends Set
case class Union(@(JsonProperty @field)("union") sets: Array[Set]) extends Set
case class Intersection(@(JsonProperty @field)("intersection") sets: Array[Set]) extends Set
case class Difference(@(JsonProperty @field)("difference") sets: Array[Set]) extends Set
case class Join(@(JsonProperty @field)("join") source: Set, @(JsonProperty @field)("with") target: String) extends Set

object SetDeserializer {
  val SetClasses = Map(
    "match" -> classOf[Match],
    "union" -> classOf[Union],
    "intersection" -> classOf[Intersection],
    "difference" -> classOf[Difference],
    "join" -> classOf[Join]
  )
}

class SetDeserializerModifier extends BeanDeserializerModifier {
  val setDeserializer = new SetDeserializer

  override def modifyDeserializer(config: DeserializationConfig, beanDesc: BeanDescription, deserializer: JsonDeserializer[_]): JsonDeserializer[_] = {
    if (beanDesc.getBeanClass == classOf[Set])
      setDeserializer
    else
      deserializer
  }
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
