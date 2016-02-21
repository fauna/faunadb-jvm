package faunadb.values

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.node.ObjectNode

private[values] class LazyValueDeserializer extends JsonDeserializer[LazyValue] {
  override def deserialize(jsonParser: JsonParser, ctxt: DeserializationContext): LazyValue = {
    val json = jsonParser.getCodec.asInstanceOf[ObjectMapper]
    val tree = json.readTree(jsonParser).asInstanceOf[JsonNode]
    new LazyValue(tree, json)
  }
}

private[values] class LazyValueMapDeserializer extends JsonDeserializer[LazyValueMap] {
  override def deserialize(jsonParser: JsonParser, ctxt: DeserializationContext): LazyValueMap = {
    val json = jsonParser.getCodec.asInstanceOf[ObjectMapper]
    val tree = json.readTree(jsonParser).asInstanceOf[JsonNode]

    val innerTree = if(tree.has("@obj"))
      tree.get("@obj").asInstanceOf[ObjectNode]
    else
      tree

    val t = ctxt.getTypeFactory.constructMapLikeType(classOf[Map[_,_]], classOf[String], classOf[LazyValue])
    new LazyValueMap(json.convertValue(innerTree, t))
  }
}

private[values] class TsDeserializer extends JsonDeserializer[Ts] {
  override def deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): Ts = {
    val json = jsonParser.getCodec.asInstanceOf[ObjectMapper]
    val tree = json.readTree(jsonParser).asInstanceOf[JsonNode]
    new Ts(tree.get("@ts").asText)
  }
}

private[values] class DateDeserializer extends JsonDeserializer[Date] {
  override def deserialize(jsonParser: JsonParser, deserializationContext: DeserializationContext): Date = {
    val json = jsonParser.getCodec.asInstanceOf[ObjectMapper]
    val tree = json.readTree(jsonParser).asInstanceOf[JsonNode]
    new Date(tree.get("@date").asText)
  }
}
