package com.faunadb.client.errors;

/**
 * An exception thrown if a HTTP 500 (Internal Server Error) occurs when making a request to FaunaDB. Such
 * errors represent an internal failure within the database.
 */
public class InternalException extends FaunaException {
  public InternalException(String message) {
    super(message);
  }
}
