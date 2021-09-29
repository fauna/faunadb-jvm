package com.faunadb.client.errors;

import com.faunadb.client.HttpResponses;

/**
 * An exception thrown if a HTTP 404 (Not Found) is returned from FaunaDB.
 */
public class NotFoundException extends FaunaException {
  public NotFoundException(String message, int httpStatusCode) {
    super(message, httpStatusCode);
  }
}
