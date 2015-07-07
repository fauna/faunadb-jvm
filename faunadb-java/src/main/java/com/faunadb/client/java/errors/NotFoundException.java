package com.faunadb.client.java.errors;

import com.faunadb.client.java.HttpResponses;

/**
 * An exception thrown if a HTTP 404 (Not Found) is returned from FaunaDB.
 */
public class NotFoundException extends FaunaException {
  public NotFoundException(HttpResponses.QueryErrorResponse response) {
    super(response);
  }

  public NotFoundException(String message) {
    super(message);
  }
}
