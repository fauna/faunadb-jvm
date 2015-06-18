package com.faunadb.client.java.query;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.faunadb.client.java.types.Identifier;

/**
 * An immutable representation of a FaunaDB Get function.
 *
 * <p><i>Reference</i>: <a href="https://faunadb.com/documentation#queries-reading-resources">FaunaDB Resource Retrieval Functions</a></p>
 *
 * @see Language#Get(Identifier)
 */
public final class Get implements Expression {
  @JsonProperty("get")
  private final Identifier resource;

  public static Get create(Identifier resource) {
    return new Get(resource);
  }

  Get(Identifier resource) {
    this.resource = resource;
  }

  public Identifier resource() {
    return resource;
  }
}