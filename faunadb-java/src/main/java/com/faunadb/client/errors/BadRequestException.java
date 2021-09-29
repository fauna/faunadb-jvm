package com.faunadb.client.errors;

/**
 * An exception thrown if FaunaDB cannot evaluate a query.
 */
public class BadRequestException extends FaunaException {
  public BadRequestException(String message) {
    super(message);
  }
}
