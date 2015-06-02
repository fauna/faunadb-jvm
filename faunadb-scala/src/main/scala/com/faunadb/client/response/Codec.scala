package com.faunadb.client.response

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.node.ObjectNode

class ResponseNodeDeserializer extends JsonDeserializer[ResponseNode] {
  override def deserialize(jsonParser: JsonParser, ctxt: DeserializationContext): ResponseNode = {
    val json = jsonParser.getCodec.asInstanceOf[ObjectMapper]
    val tree = json.readTree(jsonParser).asInstanceOf[JsonNode]
    new ResponseNode(tree, json)
  }
}

class ResponseMapDeserializer extends JsonDeserializer[ResponseMap] {
  override def deserialize(jsonParser: JsonParser, ctxt: DeserializationContext): ResponseMap = {
    val json = jsonParser.getCodec.asInstanceOf[ObjectMapper]
    val tree = json.readTree(jsonParser).asInstanceOf[JsonNode]

    val innerTree = if(tree.has("@obj"))
      tree.get("@obj").asInstanceOf[ObjectNode]
    else
      tree

    val t = ctxt.getTypeFactory.constructMapLikeType(classOf[Map[_,_]], classOf[String], classOf[ResponseNode])
    new ResponseMap(json.convertValue(innerTree, t))
  }
}
