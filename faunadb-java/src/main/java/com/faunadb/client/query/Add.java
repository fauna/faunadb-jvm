package com.faunadb.client.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.faunadb.client.types.Value;
import com.google.common.collect.ImmutableList;

/**
 * An add function.
 *
 * <p><i>Reference</i>: <a href="https://faunadb.com/documentation#queries-misc_functions">FaunaDB Miscellaneous Functions</a>
 */
public final class Add extends Value.ConcreteValue implements Expression {
  /**
   * Obtains a new instance of this class.
   *
   * @see Language#Add(ImmutableList)
   */
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
