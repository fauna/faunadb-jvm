package com.faunadb.client.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;

/**
 * A Concat function.
 *
 * <p><i>Reference</i>: <a href="https://faunadb.com/documentation#queries-misc_functions">FaunaDB Miscellaneous Functions</a>
 */
public final class Concat implements Expression {
  /**
   * Obtains a new instance of this class.
   *
   * @see Language#Concat
   */
  public static Concat create(ImmutableList<Expression> terms) {
    return new Concat(terms);
  }

  @JsonProperty("concat")
  private final ImmutableList<Expression> terms;

  Concat(ImmutableList<Expression> terms) {
    this.terms = terms;
  }

  public ImmutableList<Expression> terms() {
    return terms;
  }
}
