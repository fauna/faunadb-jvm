package com.faunadb.client.java.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.faunadb.client.java.types.Identifier;
import com.google.common.collect.ImmutableList;

public class Difference extends Set {
  public static Difference create(ImmutableList<Set> sets) {
    return new Difference(sets);
  }

  @JsonProperty("difference")
  private final ImmutableList<Set> sets;

  Difference(ImmutableList<Set> sets) {
    this.sets = sets;
  }
}
