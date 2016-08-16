package com.faunadb.client.errors;

import com.faunadb.client.HttpResponses;

/**
 * An exception thrown if FaunaDB responds with an HTTP 401 (Unauthorized).
 */
public class UnauthorizedException extends FaunaException {
  public UnauthorizedException(HttpResponses.QueryErrorResponse response) {
    super(response);
  }
}
