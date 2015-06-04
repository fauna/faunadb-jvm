package com.faunadb.client.java;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.faunadb.client.java.query.Expression;
import com.faunadb.httpclient.Connection;
import com.google.common.util.concurrent.ListenableFuture;

public class FaunaClient {
  public static FaunaClient create(Connection connection) {
    return new FaunaClient(connection, new ObjectMapper());
  }

  public static FaunaClient create(Connection connection, ObjectMapper json) {
    return new FaunaClient(connection, json.copy());
  }

  private final Connection connection;
  private final ObjectMapper json;

  FaunaClient(Connection connection, ObjectMapper json) {
    this.connection = connection;
    this.json = json;
  }

  public void close() {
    connection.close();
  }
}
