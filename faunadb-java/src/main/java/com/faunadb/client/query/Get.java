package com.faunadb.client.query;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An immutable representation of a FaunaDB Get function.
 *
 * <p><i>Reference</i>: <a href="https://faunadb.com/documentation#queries-reading-resources">FaunaDB Resource Retrieval Functions</a></p>
 *
 * @see Language#Get(Expression)
 */
public final class Get implements Expression {
  @JsonProperty("get")
  private final Expression resource;

  public static Get create(Expression resource) {
    return new Get(resource);
  }

  Get(Expression resource) {
    this.resource = resource;
  }

  public Expression resource() {
    return resource;
  }
}