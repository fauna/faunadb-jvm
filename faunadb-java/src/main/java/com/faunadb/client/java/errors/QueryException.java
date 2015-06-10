package com.faunadb.client.java.errors;

import com.faunadb.client.java.HttpResponses;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

public class QueryException extends FaunaException {
  public QueryException(HttpResponses.QueryErrorResponse response) {
    super(constructErrorMessage(response.errors()));
  }

  private static String constructErrorMessage(ImmutableList<HttpResponses.QueryError> errors) {
    ImmutableList.Builder<String> messages = ImmutableList.builder();
    for (HttpResponses.QueryError error : errors) {
      messages.add(error.code() + ": " + error.reason());
    }

    return Joiner.on(", ").join(messages.build());
  }
}
