package com.faunadb.client.java.errors;

public abstract class FaunaException extends RuntimeException {
  public FaunaException(String message) {
    super(message);
  }
}
