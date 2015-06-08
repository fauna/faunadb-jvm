package com.faunadb.client.java.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.faunadb.client.java.types.Ref;
import com.google.common.collect.ImmutableList;

public class ResponseNode {
  private final JsonNode underlying;
  private final ObjectMapper json;

  ResponseNode(JsonNode underlying, ObjectMapper json) {
    this.underlying = underlying;
    this.json = json;
  }

  public String asString() {
    return underlying.asText();
  }

  public Long asNumber() {
    if (underlying.isNumber()) {
      return underlying.asLong();
    } else {
      return null;
    }
  }

  public Double asDouble() {
    if (underlying.isDouble()) {
      return underlying.asDouble()
    } else {
      return null;
    }
  }

  public ImmutableList<ResponseNode> asArray() {
    json.convertValue(underlying, TypeFactory.defaultInstance().constructCollectionLikeType(ImmutableList.class, ResponseNode.class));
  }

  public ResponseMap asObject() {
    json.convertValue(underlying, ResponseMap.class);
  }

  public Ref asRef() {
    json.convertValue(underlying, Ref.class);
  }
}
