package com.faunadb.client.types;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import static com.faunadb.client.types.Value.ArrayV.*;

class Deserializer {

  private static abstract class TreeDeserializer<T> extends JsonDeserializer<T> {
    @Override
    public T deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException {
      JsonLocation location = jsonParser.getTokenLocation();
      ObjectMapper json = (ObjectMapper) jsonParser.getCodec();
      JsonNode tree = json.readTree(jsonParser);

      return deserializeTree(tree, json, location);
    }

    abstract T deserializeTree(
      JsonNode tree, ObjectMapper json, JsonLocation loc) throws JsonParseException;
  }

  static class ValueDeserializer extends TreeDeserializer<Value> {
    @Override
    Value deserializeTree(JsonNode tree, ObjectMapper json, JsonLocation loc)
      throws JsonParseException {

      switch (tree.getNodeType()) {
        case OBJECT:
          return deserializeSpecial(tree, json);
        case ARRAY:
          return json.convertValue(tree, ArrayV.class);
        case STRING:
          return json.convertValue(tree, StringV.class);
        case BOOLEAN:
          return json.convertValue(tree, BooleanV.class);
        case NUMBER:
          return tree.isDouble() ?
            json.convertValue(tree, DoubleV.class) :
            json.convertValue(tree, LongV.class);
        case NULL:
          return NullV.NULL;
        default:
          throw new JsonParseException("Cannot deserialize as a Value", loc);
      }
    }

    private Value deserializeSpecial(JsonNode tree, ObjectMapper json) {
      if (tree.size() == 0)
        return json.convertValue(tree, ObjectV.class);

      String firstField = tree.fieldNames().next();

      switch (firstField) {
        case "@ref":
          return json.convertValue(tree, RefV.class);
        case "@set":
          return json.convertValue(tree, SetRefV.class);
        case "@ts":
          return json.convertValue(tree, TimeV.class);
        case "@date":
          return json.convertValue(tree, DateV.class);
        case "@bytes":
          return json.convertValue(tree, BytesV.class);
        case "@query":
          return json.convertValue(tree, QueryV.class);
        case "@obj":
          return json.convertValue(tree.get("@obj"), ObjectV.class);
        default:
          return json.convertValue(tree, ObjectV.class);
      }
    }
  }

  static class ArrayDeserializer extends TreeDeserializer<ArrayV> {
    @Override
    ArrayV deserializeTree(JsonNode tree, final ObjectMapper json, JsonLocation loc)
      throws JsonParseException {

      ImmutableList.Builder<Value> values = ImmutableList.builder();

      for (Iterator<JsonNode> elements = tree.elements(); elements.hasNext(); ) {
        values.add(toValueOrNullV(elements.next(), json));
      }

      return new ArrayV(values.build());
    }

  }

  static class ObjectDeserializer extends TreeDeserializer<ObjectV> {
    @Override
    ObjectV deserializeTree(final JsonNode tree, final ObjectMapper json, JsonLocation loc)
      throws JsonParseException {

      ImmutableMap.Builder<String, Value> values = ImmutableMap.builder();

      for (Iterator<Map.Entry<String, JsonNode>> entries = tree.fields(); entries.hasNext(); ) {
        Map.Entry<String, JsonNode> entry = entries.next();
        values.put(entry.getKey(), toValueOrNullV(entry.getValue(), json));
      }

      return new ObjectV(values.build());
    }
  }

  private static Value toValueOrNullV(JsonNode node, ObjectMapper json) {
    Value value = json.convertValue(node, Value.class);
    return value != null ? value : NullV.NULL;
  }

}
