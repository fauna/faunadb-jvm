package com.faunadb.client.java.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.faunadb.client.java.types.Ref;
import com.google.common.collect.ImmutableList;

import javax.activation.UnsupportedDataTypeException;

@JsonDeserialize(using=Codec.ResponseNodeDeserializer.class)
public class ResponseNode {
  public static ResponseNode create(JsonNode underlying, ObjectMapper json) {
    return new ResponseNode(underlying, json);
  }

  private final JsonNode underlying;
  private final ObjectMapper json;

  @JsonCreator
  ResponseNode(JsonNode underlying, ObjectMapper json) {
    this.underlying = underlying;
    this.json = json;
  }

  public String asString() {
    return underlying.asText();
  }

  public Boolean asBoolean() {
    if (underlying.isBoolean()) {
      return underlying.asBoolean();
    } else {
      return null;
    }
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
      return underlying.asDouble();
    } else {
      return null;
    }
  }

  public ImmutableList<ResponseNode> asArray() {
    return json.convertValue(underlying, TypeFactory.defaultInstance().constructCollectionType(ImmutableList.class, ResponseNode.class));
  }

  public ResponseMap asObject() {
    return json.convertValue(underlying, ResponseMap.class);
  }

  public Ref asRef() {
    return json.convertValue(underlying, Ref.class);
  }

  public Page asPage() {
    return json.convertValue(underlying, Page.class);
  }

  public Instance asInstance() {
    return json.convertValue(underlying, Instance.class);
  }

  public Key asKey() {
    return json.convertValue(underlying, Key.class);
  }

  public Database asDatabase() {
    return json.convertValue(underlying, Database.class);
  }

  public Class asClass() {
    return json.convertValue(underlying, Class.class);
  }

  public Index asIndex() {
    return json.convertValue(underlying, Index.class);
  }

  @Override
  public boolean equals(Object obj) {
    return (obj instanceof ResponseNode) && underlying.equals(((ResponseNode) obj).underlying);
  }

  @Override
  public int hashCode() {
    return underlying.hashCode();
  }

  @Override
  public String toString() {
    return underlying.toString();
  }
}
