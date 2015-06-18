package com.faunadb.client.java.errors;

import com.faunadb.client.java.HttpResponses;

/**
 * An exception thrown if a query error is unknown or unparseable by the client.
 */
public class UnknownQueryException extends QueryException {
  public UnknownQueryException(HttpResponses.QueryErrorResponse response) {
    super(response);
  }
}
