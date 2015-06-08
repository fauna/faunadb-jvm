package com.faunadb.client.java.query;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Lambda {
  public static Lambda create(String argument, Expression expr) {
    return new Lambda(argument, expr);
  }

  @JsonProperty("lambda")
  private final String argument;
  @JsonProperty("expr")
  private final Expression expr;

  Lambda(String argument, Expression expr) {
    this.argument = argument;
    this.expr = expr;
  }
}
