package com.faunadb.client.java.errors;

/**
 * The base type for all FaunaDB exceptions.
 */
public abstract class FaunaException extends RuntimeException {
  public FaunaException(String message) {
    super(message);
  }
}
