package com.faunadb.client.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.faunadb.client.types.Value;
import com.google.common.collect.ImmutableList;

/**
 * A Subtract function
 *
 * <p><i>Reference</i>: <a href="https://faunadb.com/documentation#queries-misc_functions">FaunaDB Miscellaneous Functions</a>
 */
public final class Subtract extends Value implements Expression {
  /**
   * Obtains a new instance of this class.
   *
   * @see Language#Subtract(ImmutableList)
   */
  public static Subtract create(ImmutableList<Expression> terms) {
    return new Subtract(terms);
  }

  @JsonProperty("subtract")
  private ImmutableList<Expression> terms;

  Subtract(ImmutableList<Expression> terms) {
    this.terms = terms;
  }

  public ImmutableList<Expression> terms() {
    return terms;
  }

}
