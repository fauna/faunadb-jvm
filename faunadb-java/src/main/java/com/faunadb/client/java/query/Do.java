package com.faunadb.client.java.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;

public class Do implements Expression {
  public static Do create(ImmutableList<Expression> expressions) {
    return new Do(expressions);
  }

  @JsonProperty("do")
  private final ImmutableList<Expression> expressions;

  Do(ImmutableList<Expression> expressions) {
    this.expressions = expressions;
  }
}
