package com.faunadb.client.java.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.faunadb.client.java.types.Identifier;

public class Get implements Expression {
  @JsonProperty("get")
  private final Identifier resource;

  public static Get create(Identifier resource) {
    return new Get(resource);
  }

  Get(Identifier resource) {
    this.resource = resource;
  }
}