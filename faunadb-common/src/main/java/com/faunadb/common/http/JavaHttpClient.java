package com.faunadb.common.http;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.*;

public class JavaHttpClient implements AutoCloseable {

  private HttpClient _client;

  public JavaHttpClient(int connectionTimeout) {
    // TODO: [DRV-169] allow users to override default executor
    this._client = HttpClient.newBuilder()
      .connectTimeout(Duration.ofMillis(connectionTimeout))
      .build();
  }

  public void close() {
    // Garbage Collector frees any associated resources
    // when setting the reference to the HttpClient to null.
    _client = null;
  }

  /**
   * Verifies if the client still accepts new requests.
   *
   * @return true if closed, false if not
   * @see #close()
   */
  public boolean isClosed() {
    return _client == null;
  }

  public CompletableFuture<HttpResponse<String>> sendRequest(HttpRequest req) {
    if (isClosed()) {
      return CompletableFuture.failedFuture(new IllegalStateException("Client already closed"));
    }

    return _client.sendAsync(req, HttpResponse.BodyHandlers.ofString());
  }

  // TODO expose on the connection
  public CompletableFuture<HttpResponse<Void>> streamRequest(HttpRequest req, Flow.Subscriber<? super String> subscription) {
    if (isClosed()) {
      return CompletableFuture.failedFuture(new IllegalStateException("Client already closed"));
    }

    return _client.sendAsync(req, HttpResponse.BodyHandlers.fromLineSubscriber(subscription));
  }


}
