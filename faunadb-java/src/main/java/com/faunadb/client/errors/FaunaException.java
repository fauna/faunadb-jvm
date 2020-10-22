package com.faunadb.client.errors;

import com.faunadb.client.HttpResponses;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * The base type for all FaunaDB exceptions.
 */
public class FaunaException extends RuntimeException {
  private final Optional<HttpResponses.QueryErrorResponse> response;

  public FaunaException(HttpResponses.QueryErrorResponse response) {
    super(constructErrorMessage(response.errors()));
    this.response = Optional.of(response);
  }

  public FaunaException(String message) {
    super(message);
    this.response = Optional.empty();
  }

  public FaunaException(String message, Throwable cause) {
    super(message, cause);
    this.response = Optional.empty();
  }

  /**
   * Gets the list of errors that caused the query to fail.
   *
   * @return a list of errors
   */
  public List<HttpResponses.QueryError> errors() {
    if (response.isPresent()) {
      return response.get().errors();
    } else {
      return Collections.emptyList();
    }
  }

  /**
   * Gets the HTTP status code of the underlying error response.
   *
   * @return HTTP status code
   */
  public int status() {
    if (response.isPresent()) {
      return response.get().status();
    } else {
      return 0;
    }
  }

  private static String constructErrorMessage(List<HttpResponses.QueryError> errors) {
    List<String> messages = new ArrayList<>();

    for (HttpResponses.QueryError error : errors) {
      messages.add(error.code() + ": " + error.description());
    }

    return String.join(", ", messages);
  }
}
