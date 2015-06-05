package com.faunadb.client.java.query;

public class Quote {
  public static Quote create(Expression expression) {
    return new Quote(expression);
  }

  private final Expression expression;

  Quote(Expression expression) {
    this.expression = expression;
  }
}
