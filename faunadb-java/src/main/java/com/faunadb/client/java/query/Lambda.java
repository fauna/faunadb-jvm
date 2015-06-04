package com.faunadb.client.java.query;

public class Lambda {
  private final String argument;
  private final Expression expr;

  public static Lambda create(String argument, Expression expr) {
    return new Lambda(argument, expr);
  }

  Lambda(String argument, Expression expr) {
    this.argument = argument;
    this.expr = expr;
  }
}
