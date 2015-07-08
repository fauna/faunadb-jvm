package com.faunadb.client.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;

/**
 * An immutable representation of a FaunaDB Union set.
 *
 * <p><i>Reference</i>: <a href="https://faunadb.com/documentation#queries-sets">FaunaDB Sets</a></p>
 *
 * @see Language#Union(ImmutableList)
 */
public final class Union extends Set {
  public static Union create(ImmutableList<Set> sets) {
    return new Union(sets);
  }

  @JsonProperty("union")
  private final ImmutableList<Set> sets;

  Union(ImmutableList<Set> sets) {
    this.sets = sets;
  }

  public ImmutableList<Set> sets() {
    return sets;
  }
}
