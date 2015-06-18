package com.faunadb.client.java.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;

/**
 * An immutable representation of a FaunaDB Difference set.
 *
 * <p><i>Reference</i>: <a href="https://faunadb.com/documentation#queries-sets">FaunaDB Sets</a></p>
 *
 * @see Language#Difference(ImmutableList)
 */
public final class Difference extends Set {
  public static Difference create(ImmutableList<Set> sets) {
    return new Difference(sets);
  }

  @JsonProperty("difference")
  private final ImmutableList<Set> sets;

  Difference(ImmutableList<Set> sets) {
    this.sets = sets;
  }

  public ImmutableList<Set> sets() {
    return sets;
  }
}
