package com.faunadb.client.java.errors;

import com.faunadb.client.java.HttpResponses;

/**
 * A query exception thrown if a query responds with an HTTP 404 (Not Found).
 */
public class NotFoundQueryException extends QueryException {
  public NotFoundQueryException(HttpResponses.QueryErrorResponse response) {
    super(response);
  }
}
