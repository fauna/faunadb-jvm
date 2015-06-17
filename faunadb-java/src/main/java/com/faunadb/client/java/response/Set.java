package com.faunadb.client.java.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Set {
  public static Set create(ResponseMap parameters) {
    return new Set(parameters);
  }

  @JsonProperty("@set")
  private final ResponseMap parameters;

  @JsonCreator
  Set(@JsonProperty("@set") ResponseMap parameters) {
    this.parameters = parameters;
  }

  public ResponseMap parameters() {
    return parameters;
  }

  @Override
  public String toString() {
    return "Set(" + parameters().toString() + ")";
  }
}
