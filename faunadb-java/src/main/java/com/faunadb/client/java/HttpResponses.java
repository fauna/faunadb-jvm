package com.faunadb.client.java;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class HttpResponses {
  public static class ParamError {
    private final String error;
    private final String reason;

    @JsonCreator
    public ParamError(
      @JsonProperty("error") String error,
      @JsonProperty("reason") String reason) {
      this.error = error;
      this.reason = reason;
    }

    public String error() {
      return error;
    }

    public String reason() {
      return reason;
    }
  }

  public static class QueryError {
    private final ImmutableList<String> position;
    private final String code;
    private final String reason;
    private final ImmutableMap<String, ParamError> parameters;

    @JsonCreator
    public QueryError(@JsonProperty("position") ImmutableList<String> position,
                      @JsonProperty("code") String code,
                      @JsonProperty("reason") String reason,
                      @JsonProperty("parameters") ImmutableMap<String, ParamError> parameters) {
      this.position = position;
      this.code = code;
      this.reason = reason;
      this.parameters = parameters;
    }

    public ImmutableList<String> position() {
      return position;
    }

    public String code() {
      return code;
    }

    public String reason() {
      return reason;
    }

    public ImmutableMap<String, ParamError> parameters() {
      return parameters;
    }
  }

  public static class ErrorResponse {
    private final int status;
    private final String error;

    public ErrorResponse(int status, String error) {
      this.status = status;
      this.error = error;
    }

    public int status() {
      return status;
    }

    public String error() {
      return error;
    }
  }

  public static class QueryErrorResponse {
    public static QueryErrorResponse create(int status, ImmutableList<QueryError> errors) {
      return new QueryErrorResponse(status, errors);
    }

    private final int status;
    private final ImmutableList<QueryError> errors;

    QueryErrorResponse(int status, ImmutableList<QueryError> errors) {
      this.status = status;
      this.errors = errors;
    }

    public int status() {
      return status;
    }

    public ImmutableList<QueryError> errors() {
      return errors;
    }
  }
}
