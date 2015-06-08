package com.faunadb.client.java.query;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Quote {
  public static Quote create(Expression expression) {
    return new Quote(expression);
  }

  @JsonProperty("quote")
  private final Expression expression;

  Quote(Expression expression) {
    this.expression = expression;
  }
}
