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
 * An abstract node in a FaunaDB response tree. An instance of this class does not have any accessible data. It must
 * first be coerced into a concrete response type, or a concrete value type.
 *
 * <p>Coercion functions will return null if this node cannot be transformed into the requested type.
 *
 * <p><b>Example</b>: Consider the {@code LazyValue node} modeling the root of the tree:</p>
 * <pre>
 * {
 *   "ref": { "@ref": "some/ref" },
 *   "data": { "someKey": "string1", "someKey2": 123 }
 * }</pre>
 *
 * <p>The result tree can be accessed using:</p>
 *
 * <pre>
 *   node.get("ref").asRef(); // {@link Ref}("some/ref")
 *   node.get("data").get("someKey").asString() // "string1"
 * </pre>
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

  /**
   * Coerces this node into a {@link String}.
   * @return the string value of this node, or null.
   */
  public String asString() {
    if (underlying.isTextual()) {
      return underlying.asText();
    } else {
      return null;
    }
  }

  /**
   * Coerces this node into a {@link Boolean}.
   * @return the boolean value of this node, or null.
   */
  public Boolean asBoolean() {
    if (underlying.isBoolean()) {
      return underlying.asBoolean();
    } else {
      return null;
    }
  }

  /**
   * Coerces this node into a {@link Long}.
   * @return the long value of this node, or null.
   */
  public Long asLong() {
    if (underlying.isNumber()) {
      return underlying.asLong();
    } else {
      return null;
    }
  }

  /**
   * Coerces this node into a {@link Double}.
   * @return the double value of this node, or null.
   */
  public Double asDouble() {
    if (underlying.isDouble()) {
      return underlying.asDouble();
    } else {
      return null;
    }
  }

  /**
   * Coerces this node into an ordered list of nodes.
   * @return an ordered list of response nodes, or null.
   */
  public ImmutableList<Value> asArray() {
    try {
      return json.convertValue(underlying, TypeFactory.defaultInstance().constructCollectionType(ImmutableList.class, LazyValue.class));
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  /**
   * Coerces this node into a dictionary of nodes.
   * @return a dictionary of nodes, or null.
   */
  public ImmutableMap<String, Value> asObject() {
    try {
      return ImmutableMap.copyOf(json.convertValue(underlying, LazyValueMap.class));
    } catch (ClassCastException ex) {
      return null;
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  /**
   * Coerces this node into a {@link Ref}.
   * @return a Ref, or null.
   */
  public Ref asRef() {
    try {
      return json.convertValue(underlying, Ref.class);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  /**
   * Coerces this node into a {@link Page}.
   * @return a Page, or null.
   */
  public Page asPage() {
    try {
      return json.convertValue(underlying, Page.class);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  /**
   * Coerces this node into an {@link Instance}.
   * @return an Instance, or null.
   */
  public Instance asInstance() {
    try {
      return json.convertValue(underlying, Instance.class);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  /**
   * Coerces this node into a {@link Key}.
   * @return a Key, or null.
   */
  public Key asKey() {
    try {
      return json.convertValue(underlying, Key.class);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  /**
   * Coerces this node into a {@link Database}.
   * @return a Database, or null.
   */
  public Database asDatabase() {
    try {
      return json.convertValue(underlying, Database.class);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  /**
   * Coerces this node into a {@link Class}.
   * @return a Class, or null.
   */
  public Class asClass() {
    try {
      return json.convertValue(underlying, Class.class);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  /**
   * Coerces this node into an {@link Index}.
   * @return an Index, or null.
   */
  public Index asIndex() {
    try {
      return json.convertValue(underlying, Index.class);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  /**
   * Coerces this node into an {@link Event}.
   * @return an Event, or null.
   */
  public Event asEvent() {
    try {
      return json.convertValue(underlying, Event.class);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  /**
   * Coerces this node into a {@link Set}.
   * @return a Set, or null.
   */
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
