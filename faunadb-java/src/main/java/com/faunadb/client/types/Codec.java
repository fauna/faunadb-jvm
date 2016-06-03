package com.faunadb.client.types;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.faunadb.client.types.Value.ArrayV;
import com.faunadb.client.types.Value.NullV;
import com.faunadb.client.types.Value.ObjectV;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.util.Iterator;

class Codec {

  private static abstract class TreeDeserializer<T> extends JsonDeserializer<T> {
    @Override
    public T deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException {
      JsonLocation location = jsonParser.getTokenLocation();
      ObjectMapper json = (ObjectMapper) jsonParser.getCodec();
      JsonNode tree = json.readTree(jsonParser);

      return deserializeTree(tree, json, context.getTypeFactory(), location);
    }

    abstract T deserializeTree(
      JsonNode tree, ObjectMapper json, TypeFactory type, JsonLocation loc) throws JsonParseException;
  }

  static class LazyValueDeserializer extends TreeDeserializer<LazyValue> {
    @Override
    LazyValue deserializeTree(JsonNode tree, ObjectMapper json, TypeFactory type, JsonLocation loc) {
      return new LazyValue(tree, json);
    }
  }

  static class SetRefDeserializer extends TreeDeserializer<SetRef> {
    @Override
    SetRef deserializeTree(JsonNode tree, ObjectMapper json, TypeFactory type, JsonLocation loc)
      throws JsonParseException {
      if (!tree.has("@set"))
        throw new JsonParseException("Cannot deserialize as a @set", loc);

      ImmutableMap<String, Value> values = json.convertValue(tree.get("@set"),
        type.constructMapLikeType(ImmutableMap.class, String.class, LazyValue.class)
      );

      return new SetRef(values);
    }
  }

  static class ArrayDeserializer extends TreeDeserializer<ArrayV> {
    @Override
    ArrayV deserializeTree(JsonNode tree, final ObjectMapper json, TypeFactory type, JsonLocation loc)
      throws JsonParseException {
      if (!tree.isArray())
        throw new JsonParseException("Cannot deserialize as an array", loc);

      ImmutableList.Builder<Value> values = ImmutableList.builder();
      for (Iterator<JsonNode> elements = tree.elements(); elements.hasNext(); )
        values.add(toLazyValue(elements.next(), json));

      return new ArrayV(values.build());
    }
  }

  static class ObjectDeserializer extends TreeDeserializer<ObjectV> {
    @Override
    ObjectV deserializeTree(final JsonNode tree, final ObjectMapper json, TypeFactory type, JsonLocation loc)
      throws JsonParseException {
      if (!tree.isObject())
        throw new JsonParseException("Cannot deserialize as an object", loc);

      ImmutableMap.Builder<String, Value> result = ImmutableMap.builder();
      for (Iterator<String> fields = tree.fieldNames(); fields.hasNext(); ) {
        String key = fields.next();
        result.put(key, toLazyValue(tree.get(key), json));
      }

      ImmutableMap<String, Value> values = result.build();

      if (values.containsKey("@ref")) throw new JsonParseException("Cannot deserialize Ref as an object", loc);
      if (values.containsKey("@obj")) return new ObjectV(values.get("@obj").asObject());

      return new ObjectV(values);
    }
  }

  private static Value toLazyValue(JsonNode node, ObjectMapper json) {
    if (node.isNull()) return NullV.NULL;
    return json.convertValue(node, LazyValue.class);
  }

}
