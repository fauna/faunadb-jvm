package com.faunadb.client.java.query;

public class Foreach {
  public static Foreach create(Lambda lambda, Expression collection) {
    return new Foreach(lambda, collection);
  }

  private final Lambda lambda;
  private final Expression collection;

  Foreach(Lambda lambda, Expression collection) {
    this.lambda = lambda;
    this.collection = collection;
  }
}
