package com.faunadb.client.java.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.faunadb.client.java.types.Value;

/**
 * An immutable representation of a FaunaDB Quote function.
 *
 * <p><i>Reference</i>: <a href="https://faunadb.com/documentation#queries-basic_forms">FaunaDB Basic Forms</a></p>
 *
 * @see Language#Quote(Expression)
 */
public final class Quote extends Value {
  public static Quote create(Expression expression) {
    return new Quote(expression);
  }

  @JsonProperty("quote")
  private final Expression expression;

  Quote(Expression expression) {
    this.expression = expression;
  }

  public Expression expression() {
    return expression;
  }
}
