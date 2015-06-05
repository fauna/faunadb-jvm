package com.faunadb.client.java.query;

import com.google.common.collect.ImmutableList;

public class Fetch {
  public static Fetch create(ImmutableList<Path> path, Value from) {
    return new Fetch(path, from);
  }

  private final ImmutableList<Path> path;
  private final Value from;

  Fetch(ImmutableList<Path> path, Value from) {
    this.path = path;
    this.from = from;
  }
}
