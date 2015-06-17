package com.faunadb.client.java.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.faunadb.client.java.types.Value;

public class Object extends Value {
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
