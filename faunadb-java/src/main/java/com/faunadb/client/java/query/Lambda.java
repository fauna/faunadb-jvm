package com.faunadb.client.java.query;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An immutable representation of a FaunaDB Lambda expression.
 *
 * <p><i>Reference</i>: TBD.
 *
 * @see Language#Lambda(String, Expression)
 */
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

  public String argument() {
    return argument;
  }

  public Expression expr() {
    return expr;
  }
}
