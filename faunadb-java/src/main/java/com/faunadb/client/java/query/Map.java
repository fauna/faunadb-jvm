package com.faunadb.client.java.query;

public class Map implements Expression {
  private final Lambda lambda;
  private final Expression collection;

  public static Map create(Lambda lambda, Expression collection) {
    return new Map(lambda, collection);
  }

  Map(Lambda lambda, Expression collection) {
    this.lambda = lambda;
    this.collection = collection;
  }
}
