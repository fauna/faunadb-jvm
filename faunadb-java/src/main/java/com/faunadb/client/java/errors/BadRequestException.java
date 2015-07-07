package com.faunadb.client.java.errors;

import com.faunadb.client.java.HttpResponses;

/**
 * An exception thrown if FaunaDB cannot evaluate a query.
 */
public class BadRequestException extends FaunaException {
  public BadRequestException(HttpResponses.QueryErrorResponse response) {
    super(response);
  }
}
