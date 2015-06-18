package com.faunadb.client.java.errors;

/**
 * An exception thrown if FaunaDB responds with an HTTP 401 (Unauthorized).
 */
public class UnauthorizedException extends FaunaException {
  public UnauthorizedException(String message) {
    super(message);
  }
}