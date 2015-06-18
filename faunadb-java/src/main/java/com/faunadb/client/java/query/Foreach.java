package com.faunadb.client.java.query;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * An immutable representation of a FaunaDB Foreach function.
 *
 * <p><i>Reference</i>: <a href="https://faunadb.com/documentation#queries-collection_functions">FaunaDB Collection Functions</a></p>
 *
 * @see Language#Foreach(Lambda, Expression)
 */
public class Foreach implements Expression {
  public static Foreach create(Lambda lambda, Expression collection) {
    return new Foreach(lambda, collection);
  }

  @JsonProperty("foreach")
  private final Lambda lambda;
  @JsonProperty("collection")
  private final Expression collection;

  Foreach(Lambda lambda, Expression collection) {
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
