package com.faunadb.client.java.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.faunadb.client.java.types.Value;

/**
 * An immutable representation of a FaunaDB Object function.
 *
 * <p><i>Reference</i>: <a href="https://faunadb.com/documentation#queries-basic_forms">FaunaDB Basic Forms</a></p>
 *
 * @see Language#Object()
 * @see com.faunadb.client.java.types.Value.ObjectV
 */
public final class Object extends Value {
  public static Object create(ObjectV value) {
    return new Object(value);
  }

  @JsonProperty("object")
  private final ObjectV value;

  Object(ObjectV value) {
    this.value = value;
  }

  public ObjectV value() {
    return value;
  }
}
