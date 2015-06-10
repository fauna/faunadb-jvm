package com.faunadb.client.java.errors;

import com.faunadb.client.java.HttpResponses;

public class NotFoundQueryException extends QueryException {
  public NotFoundQueryException(HttpResponses.QueryErrorResponse response) {
    super(response);
  }
}
