package com.faunadb.client.types;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.faunadb.client.types.Value.ArrayV.*;
import static java.lang.String.format;

class Deserializer {

  private static abstract class TreeDeserializer<T> extends JsonDeserializer<T> {
    @Override
    public T deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException {
      JsonLocation location = jsonParser.getTokenLocation();
      ObjectMapper json = (ObjectMapper) jsonParser.getCodec();
      JsonNode tree = json.readTree(jsonParser);

      return deserializeTree(jsonParser, tree, json, location);
    }

    abstract T deserializeTree(
      JsonParser jsonParser, JsonNode tree, ObjectMapper json, JsonLocation loc) throws JsonParseException;
  }

  static class ValueDeserializer extends TreeDeserializer<Value> {
    @Override
    Value deserializeTree(JsonParser jsonParser, JsonNode tree, ObjectMapper json, JsonLocation loc)
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
          throw new JsonParseException(jsonParser, "Cannot deserialize as a Value", loc);
      }
    }

    private Value deserializeSpecial(JsonNode tree, ObjectMapper json) {
      if (tree.size() == 0)
        return json.convertValue(tree, ObjectV.class);

      String firstField = tree.fieldNames().next();

      switch (firstField) {
        case "@ref":
          return deserializeRefs(tree);
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

    private RefV deserializeRefs(JsonNode node) {
      if (node == null)
        return null;

      JsonNode ref = node.get("@ref");

      if (ref != null)
        return makeRef(ref);

      throw new IllegalArgumentException(format("Malformed @ref: %s", node));
    }

    private RefV makeRef(JsonNode node) {
      JsonNode id = node.get("id");
      RefV collection = deserializeRefs(node.get("collection"));
      RefV database = deserializeRefs(node.get("database"));

      String idE = id.textValue();

      if (collection == null && database == null)
        return Native.fromName(idE);

      return new RefV(idE, collection, database);
    }
  }

  static class ArrayDeserializer extends TreeDeserializer<ArrayV> {
    @Override
    ArrayV deserializeTree(JsonParser jsonParser, JsonNode tree, final ObjectMapper json, JsonLocation loc) {

      List<Value> values = new ArrayList<>();

      for (Iterator<JsonNode> elements = tree.elements(); elements.hasNext(); ) {
        values.add(toValueOrNullV(elements.next(), json));
      }

      return new ArrayV(values);
    }

  }

  static class ObjectDeserializer extends TreeDeserializer<ObjectV> {
    @Override
    ObjectV deserializeTree(JsonParser jsonParser, final JsonNode tree, final ObjectMapper json, JsonLocation loc) {

      Map<String, Value> values = new LinkedHashMap<>();

      for (Iterator<Map.Entry<String, JsonNode>> entries = tree.fields(); entries.hasNext(); ) {
        Map.Entry<String, JsonNode> entry = entries.next();
        values.put(entry.getKey(), toValueOrNullV(entry.getValue(), json));
      }

      return new ObjectV(values);
    }
  }

  private static Value toValueOrNullV(JsonNode node, ObjectMapper json) {
    Value value = json.convertValue(node, Value.class);
    return value != null ? value : NullV.NULL;
  }

}
