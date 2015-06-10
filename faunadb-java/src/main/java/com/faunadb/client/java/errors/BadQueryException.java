package com.faunadb.client.java.errors;

import com.faunadb.client.java.HttpResponses;

public class BadQueryException extends QueryException {
  public BadQueryException(HttpResponses.QueryErrorResponse response) {
    super(response);
  }
}
