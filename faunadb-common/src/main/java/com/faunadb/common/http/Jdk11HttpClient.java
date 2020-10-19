package com.faunadb.common.http;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.*;

public class Jdk11HttpClient implements AutoCloseable {

  private HttpClient _client;
  private ExecutorService _executor;
  private long _requestTimeout;

  public Jdk11HttpClient(int connectionTimeout, int requestTimeout) {
    this._requestTimeout = requestTimeout;
    this._executor = Executors.newCachedThreadPool();
    this._client = HttpClient.newBuilder()
      .connectTimeout(Duration.ofMillis(connectionTimeout))
      .executor(_executor)
      .build();
  }

  public void close() {
    _executor.shutdownNow();
    _client = null;
  }

  /**
   * Verifies if the client stills accepting new requests
   *
   * @return
   * @see #close()
   */
  public boolean isClosed() {
    return _executor == null || _executor.isShutdown() || _executor.isTerminated();
  }

  public CompletableFuture<HttpResponse<String>> sendRequest(HttpRequest req) {
    if (isClosed()) {
      throw new IllegalStateException("Client already closed");
    }
    return _client.sendAsync(req, HttpResponse.BodyHandlers.ofString());
  }

  // TODO expose on the connection
  public CompletableFuture<HttpResponse<Void>> streamRequest(HttpRequest req, Flow.Subscriber<? super String> subscription) {
    if (isClosed()) {
      throw new IllegalStateException("Client already closed");
    }
    return _client.sendAsync(req, HttpResponse.BodyHandlers.fromLineSubscriber(subscription));
  }


}
