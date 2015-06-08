package com.faunadb.client.java.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;

public class Fetch {
  public static Fetch create(ImmutableList<Path> path, Value from) {
    return new Fetch(path, from);
  }

  @JsonProperty("fetch")
  private final ImmutableList<Path> path;
  @JsonProperty("from")
  private final Value from;

  Fetch(ImmutableList<Path> path, Value from) {
    this.path = path;
    this.from = from;
  }
}
