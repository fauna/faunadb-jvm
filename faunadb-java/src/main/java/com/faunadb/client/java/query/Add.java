package com.faunadb.client.java.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.faunadb.client.java.types.Value;
import com.google.common.collect.ImmutableList;

public class Add extends Value implements Expression {
  public static Add create(ImmutableList<Expression> terms) {
    return new Add(terms);
  }

  @JsonProperty("add")
  private ImmutableList<Expression> terms;

  Add(ImmutableList<Expression> terms) {
    this.terms = terms;
  }

  public ImmutableList<Expression> terms() {
    return terms;
  }

}
