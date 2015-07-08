package com.faunadb.client.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.faunadb.client.types.Identifier;

/**
 * An immutable representation of a Count function.
 *
 * <p><i>Reference</i>: <a href="https://faunadb.com/documentation#queries-reading-resources">FaunaDB Resource Retrieval Functions</a>
 */
public final class Count implements Identifier, Expression {
  /**
   * Obtains a new instance of this class.
   *
   * @see Language#Count(Set)
   */
  public final static Count create(Set set) {
    return new Count(set);
  }

  @JsonProperty("count")
  private final Set set;

  Count(Set set) {
    this.set = set;
  }

  /**
   * Returns the set to be counted.
   */
  public Set set() {
    return set;
  }
}
