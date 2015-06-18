package com.faunadb.client.java.errors;

import com.faunadb.client.java.HttpResponses;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

/**
 * A general exception in evaluating or executing a FaunaDB Query.
 */
public class QueryException extends FaunaException {
  private final HttpResponses.QueryErrorResponse response;

  public QueryException(HttpResponses.QueryErrorResponse response) {
    super(constructErrorMessage(response.errors()));
    this.response = response;
  }

  private static String constructErrorMessage(ImmutableList<HttpResponses.QueryError> errors) {
    ImmutableList.Builder<String> messages = ImmutableList.builder();
    for (HttpResponses.QueryError error : errors) {
      messages.add(error.code() + ": " + error.reason());
    }

    return Joiner.on(", ").join(messages.build());
  }

  /**
   * Gets the list of errors that caused the query to fail.
   */
  public ImmutableList<HttpResponses.QueryError> errors() {
    return response.errors();
  }

  /**
   * Gets the HTTP status code of the underlying error response.
   */
  public int status() {
    return response.status();
  }
}
