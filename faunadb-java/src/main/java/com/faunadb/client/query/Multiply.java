package com.faunadb.client.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.faunadb.client.types.Value;
import com.google.common.collect.ImmutableList;

/**
 * A Multiply function.
 *
 * <p><i>Reference</i>: <a href="https://faunadb.com/documentation#queries-misc_functions">FaunaDB Miscellaneous Functions</a>
 */
public final class Multiply extends Value implements Expression {
  /**
   * Obtains a new instance of this class.
   *
   * @see Language#Multiply(ImmutableList)
   */
  public static Multiply create(ImmutableList<Expression> terms) {
    return new Multiply(terms);
  }

  @JsonProperty("multiply")
  private ImmutableList<Expression> terms;

  Multiply(ImmutableList<Expression> terms) {
    this.terms = terms;
  }

  public ImmutableList<Expression> terms() {
    return terms;
  }

}
