package com.faunadb.client.java.query;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Map implements Expression {
  public static Map create(Lambda lambda, Expression collection) {
    return new Map(lambda, collection);
  }

  @JsonProperty("map")
  private final Lambda lambda;
  @JsonProperty("collection")
  private final Expression collection;

  Map(Lambda lambda, Expression collection) {
    this.lambda = lambda;
    this.collection = collection;
  }
}
