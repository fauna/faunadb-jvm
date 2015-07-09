package com.faunadb.client.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.faunadb.client.types.Value;
import com.google.common.collect.ImmutableList;

/**
 * A Divide function.
 *
 * <p><i>Reference</i>: <a href="https://faunadb.com/documentation#queries-misc_functions">FaunaDB Miscellaneous Functions</a>
 */
public final class Divide implements Expression {
  /**
   * Obtains a new instance of this class.
   *
   * @see Language#Divide(ImmutableList)
   */
  public static Divide create(ImmutableList<Expression> terms) {
    return new Divide(terms);
  }

  @JsonProperty("divide")
  private ImmutableList<Expression> terms;

  Divide(ImmutableList<Expression> terms) {
    this.terms = terms;
  }

  public ImmutableList<Expression> terms() {
    return terms;
  }

}
