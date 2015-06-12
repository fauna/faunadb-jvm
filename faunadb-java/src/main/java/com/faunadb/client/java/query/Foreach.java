package com.faunadb.client.java.query;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Foreach {
  public static Foreach create(Lambda lambda, Expression collection) {
    return new Foreach(lambda, collection);
  }

  @JsonProperty("foreach")
  private final Lambda lambda;
  @JsonProperty("collection")
  private final Expression collection;

  Foreach(Lambda lambda, Expression collection) {
    this.lambda = lambda;
    this.collection = collection;
  }
}
