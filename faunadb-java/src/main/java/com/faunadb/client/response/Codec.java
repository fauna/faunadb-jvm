package com.faunadb.client.response;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.MapLikeType;
import com.google.common.collect.ImmutableMap;

import java.io.IOException;

class Codec {
  public static class ResponseNodeDeserializer extends JsonDeserializer<ResponseNode> {
    @Override
    public ResponseNode deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
      ObjectMapper json = (ObjectMapper) jsonParser.getCodec();
      JsonNode tree = json.readTree(jsonParser);
      return ResponseNode.create(tree, json);
    }
  }

  public static class ResponseMapDeserializer extends JsonDeserializer<ResponseMap> {
    @Override
    public ResponseMap deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
      ObjectMapper json = (ObjectMapper) jsonParser.getCodec();
      JsonNode tree = json.readTree(jsonParser);

      ObjectNode innerTree;
      if (tree.has("@obj")) {
        innerTree = (ObjectNode) tree.get("@obj");
      } else {
        innerTree = (ObjectNode) tree;
      }

      MapLikeType t = deserializationContext.getTypeFactory().constructMapLikeType(ImmutableMap.class, String.class, ResponseNode.class);
      return new ResponseMap(json.<ImmutableMap<String, ResponseNode>>convertValue(innerTree, t));
    }
  }
}
