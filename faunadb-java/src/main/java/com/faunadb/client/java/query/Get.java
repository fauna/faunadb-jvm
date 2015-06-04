package com.faunadb.client.java.query;

import com.fasterxml.jackson.annotation.JsonProperty;

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