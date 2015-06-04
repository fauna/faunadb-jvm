package com.faunadb.client.java.types;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Ref {
  public static Ref create(String value) {
    return new Ref(value);
  }

  @JsonProperty("@ref")
  private final String value;

  Ref(String value) {
    this.value = value;
  }

  public String value() {
    return value;
  }
}
