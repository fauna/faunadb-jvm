package com.faunadb.client.types;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.MapLikeType;
import com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.io.StringWriter;

class Codec {
  public static class LazyValueDeserializer extends JsonDeserializer<LazyValue> {
    @Override
    public LazyValue deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
      ObjectMapper json = (ObjectMapper) jsonParser.getCodec();
      JsonNode tree = json.readTree(jsonParser);
      return LazyValue.create(tree, json);
    }
  }

  public static class LazyValueMapDeserializer extends JsonDeserializer<LazyValueMap> {
    @Override
    public LazyValueMap deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
      ObjectMapper json = (ObjectMapper) jsonParser.getCodec();
      JsonNode tree = json.readTree(jsonParser);

      ObjectNode innerTree;
      if (tree.has("@obj")) {
        innerTree = (ObjectNode) tree.get("@obj");
      } else {
        innerTree = (ObjectNode) tree;
      }

      Value.ObjectV intermediate = json.convertValue(innerTree, Value.ObjectV.class);
      return new LazyValueMap(intermediate.asObject());
    }
  }

  public static class ObjectDeserializer extends JsonDeserializer<Value.ObjectV> {
    @Override
    public Value.ObjectV deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
      ImmutableMap.Builder<String, Value> mapBuilder = ImmutableMap.builder();
      JsonToken t = jsonParser.getCurrentToken();
      if (t == JsonToken.START_OBJECT) {
        t = jsonParser.nextToken();
      }

      while (jsonParser.getCurrentToken() == JsonToken.FIELD_NAME) {
        String key = jsonParser.getCurrentName();
        Value value;
        t = jsonParser.nextToken();
        if (t == JsonToken.VALUE_NULL) {
          value = Value.NullV.Null;
        } else {
          value = deserializationContext.readValue(jsonParser, LazyValue.class);
        }
        mapBuilder.put(key, value);
        t = jsonParser.nextToken();
      }

      return Value.ObjectV.create(mapBuilder.build());
    }
  }
}
