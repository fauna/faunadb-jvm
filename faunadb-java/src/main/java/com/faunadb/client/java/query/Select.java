package com.faunadb.client.java.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;

public class Select {
  public static Select create(ImmutableList<Path> path, Value from) {
    return new Select(path, from);
  }

  @JsonProperty("select")
  private final ImmutableList<Path> path;
  @JsonProperty("from")
  private final Value from;

  Select(ImmutableList<Path> path, Value from) {
    this.path = path;
    this.from = from;
  }
}
