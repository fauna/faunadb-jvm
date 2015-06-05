package com.faunadb.client.java.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;

public class Union extends Set {
  public static Union create(ImmutableList<Set> sets) {
    return new Union(sets);
  }

  @JsonProperty("union")
  private final ImmutableList<Set> sets;

  Union(ImmutableList<Set> sets) {
    this.sets = sets;
  }
}
