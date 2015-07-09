package com.faunadb.client.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.faunadb.client.types.Value;
import com.google.common.collect.ImmutableList;

/**
 * An Equals function.
 *
 * <p><i>Reference</i>: <a href="https://faunadb.com/documentation#queries-misc_functions">FaunaDB Miscellaneous Functions</a>
 */
public final class Equals implements Expression {
  /**
   * Obtains a new instance of this class.
   *
   * @see Language#Equals(ImmutableList)
   */
  public static Equals create(ImmutableList<Expression> terms) {
    return new Equals(terms);
  }

  @JsonProperty("equals")
  private final ImmutableList<Expression> terms;

  Equals(ImmutableList<Expression> terms) {
    this.terms = terms;
  }

  public ImmutableList<Expression> terms() {
    return terms;
  }
}
