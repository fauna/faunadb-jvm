package com.faunadb.client.java.errors;

/**
 * An exception thrown if a HTTP 404 (Not Found) is returned from FaunaDB.
 */
public class NotFoundException extends FaunaException {
  public NotFoundException(String message) {
    super(message);
  }
}
