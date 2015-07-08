package com.faunadb.client.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.faunadb.client.types.Value;
import com.google.common.collect.ImmutableList;

/**
 * An immutable representation of a FaunaDB Select function.
 *
 * <p><i>Reference</i>: <a href="https://faunadb.com/documentation#queries-basic_forms">FaunaDB Basic Forms</a></p>
 *
 * @see Language#Select(ImmutableList, Value)
 * @see Path
 */
public final class Select implements Expression {
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

  public ImmutableList<Path> path() {
    return path;
  }

  public Value from() {
    return from;
  }
}
