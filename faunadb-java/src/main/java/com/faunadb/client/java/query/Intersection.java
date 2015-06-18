package com.faunadb.client.java.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;

/**
 * An immutable representation of a FaunaDB Intersection function.
 *
 * <p><i>Reference</i>: <a href="https://faunadb.com/documentation#queries-sets">FaunaDB Sets</a>
 *
 * @see Language#Intersection(ImmutableList)
 */
public class Intersection extends Set {
  public static Intersection create(ImmutableList<Set> sets) {
    return new Intersection(sets);
  }

  @JsonProperty("intersection")
  private final ImmutableList<Set> sets;

  Intersection(ImmutableList<Set> sets) {
    this.sets = sets;
  }

  public ImmutableList<Set> sets() {
    return sets;
  }
}
