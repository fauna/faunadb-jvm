package com.faunadb.client.errors;

import com.faunadb.client.HttpResponses;

public class TooManyRequestsException extends FaunaException {
  public TooManyRequestsException(HttpResponses.QueryErrorResponse response) {
    super(response);
  }
  public TooManyRequestsException(String message) {
    super(message);
  }
}
