package com.faunadb.client.java.errors;

import com.faunadb.client.java.HttpResponses;

/**
 * An exception thrown if FaunaDB cannot evaluate a query.
 */
public class BadQueryException extends QueryException {
  public BadQueryException(HttpResponses.QueryErrorResponse response) {
    super(response);
  }
}
