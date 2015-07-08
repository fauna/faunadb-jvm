package com.faunadb.client.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;

/**
 * An immutable representation of a FaunaDB Let expression.
 *
 * <p><i>Reference</i> <a href="https://faunadb.com/documentation#queries-basic_forms">FaunaDB Basic Forms</a>
 *
 * @see Language#Let(ImmutableMap, Expression)
 */

public final class Let implements Expression {
  public static Let create(ImmutableMap<String, Expression> vars, Expression in) {
    return new Let(vars, in);
  }

  @JsonProperty("let")
  private final ImmutableMap<String, Expression> vars;
  @JsonProperty("in")
  private final Expression in;

  Let(ImmutableMap<String, Expression> vars, Expression in) {
    this.vars = vars;
    this.in = in;
  }

  public ImmutableMap<String, Expression> vars() {
    return vars;
  }

  public Expression in() {
    return in;
  }
}
