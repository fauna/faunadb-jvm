package com.faunadb.client.java.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Set {
  public static Set create(String value) {
    return new Set(value);
  }

  @JsonProperty("@set")
  private final String value;

  @JsonCreator
  Set(@JsonProperty("@set") String value) {
    this.value = value;
  }

  public String value() {
    return value;
  }
}
