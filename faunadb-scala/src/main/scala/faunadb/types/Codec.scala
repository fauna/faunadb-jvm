package faunadb.types

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.node.ObjectNode

private[types] class LazyValueDeserializer extends JsonDeserializer[LazyValue] {
  override def deserialize(jsonParser: JsonParser, ctxt: DeserializationContext): LazyValue = {
    val json = jsonParser.getCodec.asInstanceOf[ObjectMapper]
    val tree = json.readTree(jsonParser).asInstanceOf[JsonNode]
    new LazyValue(tree, json)
  }
}

private[types] class LazyValueMapDeserializer extends JsonDeserializer[LazyValueMap] {
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
