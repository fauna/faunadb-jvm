package com.faunadb.client.java.errors;

import com.faunadb.client.java.HttpResponses;

public class UnknownQueryException extends QueryException {
  public UnknownQueryException(HttpResponses.QueryErrorResponse response) {
    super(response);
  }
}
