package com.faunadb.client.java.query;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An immutable representation of a FaunaDB Map function
 *
 * <p><i>Reference</i>: <a href="https://faunadb.com/documentation#queries-collection_functions">FaunaDB Collection Functions</a></p>
 *
 * @see Language#Map(Lambda, Expression)
 */
public final class Map implements Expression {
  public static Map create(Lambda lambda, Expression collection) {
    return new Map(lambda, collection);
  }

  @JsonProperty("map")
  private final Lambda lambda;
  @JsonProperty("collection")
  private final Expression collection;

  Map(Lambda lambda, Expression collection) {
    this.lambda = lambda;
    this.collection = collection;
  }

  public Lambda lambda() {
    return lambda;
  }

  public Expression collection() {
    return collection;
  }
}
