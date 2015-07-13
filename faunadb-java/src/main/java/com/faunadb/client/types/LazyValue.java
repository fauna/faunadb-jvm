package com.faunadb.client.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.faunadb.client.response.*;
import com.faunadb.client.response.Class;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * A {@link Value} that wraps a JSON response tree. This Value does not convert to a concrete type until one of its
 * type coercion methods is called.
 */
@JsonDeserialize(using=Codec.LazyValueDeserializer.class)
public final class LazyValue implements Value {
  public static LazyValue create(JsonNode underlying, ObjectMapper json) {
    return new LazyValue(underlying, json);
  }

  private final JsonNode underlying;
  private final ObjectMapper json;

  @JsonCreator
  LazyValue(JsonNode underlying, ObjectMapper json) {
    this.underlying = underlying;
    this.json = json;
  }

  public String asString() {
    if (underlying.isTextual()) {
      return underlying.asText();
    } else {
      return null;
    }
  }

  public Boolean asBoolean() {
    if (underlying.isBoolean()) {
      return underlying.asBoolean();
    } else {
      return null;
    }
  }

  public Long asLong() {
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

  public ImmutableList<Value> asArray() {
    try {
      return json.convertValue(underlying, TypeFactory.defaultInstance().constructCollectionType(ImmutableList.class, LazyValue.class));
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  public ImmutableMap<String, Value> asObject() {
    try {
      return ImmutableMap.copyOf(json.convertValue(underlying, LazyValueMap.class));
    } catch (ClassCastException ex) {
      return null;
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  public Ref asRef() {
    try {
      return json.convertValue(underlying, Ref.class);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  public Page asPage() {
    try {
      return json.convertValue(underlying, Page.class);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  public Instance asInstance() {
    try {
      return json.convertValue(underlying, Instance.class);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  public Key asKey() {
    try {
      return json.convertValue(underlying, Key.class);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  public Database asDatabase() {
    try {
      return json.convertValue(underlying, Database.class);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  public Class asClass() {
    try {
      return json.convertValue(underlying, Class.class);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  public Index asIndex() {
    try {
      return json.convertValue(underlying, Index.class);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  public Event asEvent() {
    try {
      return json.convertValue(underlying, Event.class);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  public Set asSet() {
    try {
      return json.convertValue(underlying, Set.class);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  /**
   * Accesses the value of the specified field if this is an object node.
   * @return the value of the field, or null.
   */
  public Value get(String key) {
    return asObject().get(key);
  }

  /**
   * Accesses the value of the specified element if this is an array node.
   * @return the value of the element, or null.
   */
  public Value get(int index) {
    return asArray().get(index);
  }

  @Override
  public boolean equals(Object obj) {
    return (obj instanceof LazyValue) && underlying.equals(((LazyValue) obj).underlying);
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
