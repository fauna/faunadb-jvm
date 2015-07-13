package com.faunadb.client.types;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;

/**
 * A FaunaDB set literal.
 *
 * <p><b>Reference</b>: <a href="https://faunadb.com/documentation#queries-values-special_types">FaunaDB Special Types</a></p>
 *
 */
public class Set {
  public static Set create(ImmutableMap<String, Value> parameters) {
    return new Set(parameters);
  }

  @JsonProperty("@set")
  private final ImmutableMap<String, Value> parameters;

  @JsonCreator
  Set(@JsonProperty("@set") ImmutableMap<String, Value> parameters) {
    this.parameters = parameters;
  }

  public ImmutableMap<String, Value> parameters() {
    return parameters;
  }

  @Override
  public String toString() {
    return "Set(" + parameters().toString() + ")";
  }
}
