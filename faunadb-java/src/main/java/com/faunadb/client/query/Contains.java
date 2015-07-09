package com.faunadb.client.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;

/**
 * A Contains function.
 *
 * <p><i>Reference</i>: <a href="https://faunadb.com/documentation#queries-misc_functions">FaunaDB Miscellaneous Functions</a>
 */
public final class Contains implements Expression {
  /**
   * Obtains a new instance of this class.
   *
   * @see Language#Contains(ImmutableList, Expression)
   */
  public static Contains create(ImmutableList<Path> path, Expression in) {
    return new Contains(path, in);
  }

  @JsonProperty("contains")
  private final ImmutableList<Path> path;

  @JsonProperty("in")
  private final Expression in;

  Contains(ImmutableList<Path> path, Expression in) {
    this.path = path;
    this.in = in;
  }

  public ImmutableList<Path> path() {
    return path;
  }

  public Expression in() {
    return in;
  }
}
