package com.faunadb.client.java.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.faunadb.client.java.types.Value;

public class Quote extends Value {
  public static Quote create(Expression expression) {
    return new Quote(expression);
  }

  @JsonProperty("quote")
  private final Expression expression;

  Quote(Expression expression) {
    this.expression = expression;
  }
}
