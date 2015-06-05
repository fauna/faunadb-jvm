package com.faunadb.client.java.query;

import com.google.common.collect.ImmutableList;

public class Do implements Expression {
  public static Do create(ImmutableList<Expression> expressions) {
    return new Do(expressions);
  }

  private final ImmutableList<Expression> expressions;

  Do(ImmutableList<Expression> expressions) {
    this.expressions = expressions;
  }
}
