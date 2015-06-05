package com.faunadb.client.java.query;

import com.google.common.collect.ImmutableMap;

public class Let implements Expression {
  public static Let create(ImmutableMap<String, Expression> vars, Expression in) {
    return new Let(vars, in);
  }

  private final ImmutableMap<String, Expression> vars;
  private final Expression in;

  Let(ImmutableMap<String, Expression> vars, Expression in) {
    this.vars = vars;
    this.in = in;
  }
}
