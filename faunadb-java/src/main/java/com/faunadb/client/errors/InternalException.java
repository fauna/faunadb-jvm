package com.faunadb.client.errors;

import com.faunadb.client.HttpResponses;

/**
 * An exception thrown if a HTTP 500 (Internal Server Error) occurs when making a request to FaunaDB. Such
 * errors represent an internal failure within the database.
 */
public class InternalException extends FaunaException {
  public InternalException(HttpResponses.QueryErrorResponse response) {
    super(response);
  }
}
