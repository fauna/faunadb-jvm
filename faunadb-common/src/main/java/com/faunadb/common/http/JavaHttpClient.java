package com.faunadb.common.http;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.*;

public class JavaHttpClient implements AutoCloseable {

  private HttpClient _client;
  private ExecutorService _executor;

  public JavaHttpClient(int connectionTimeout) {
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
   * Verifies if the client still accepts new requests
   *
   * @return
   * @see #close()
   */
  public boolean isClosed() {
    return _executor == null || _executor.isShutdown() || _executor.isTerminated();
  }

  public CompletableFuture<HttpResponse<String>> sendRequest(HttpRequest req) {
    if (isClosed()) {
      // TODO: [DRV-174] do not throw the exception in the calling thread,
      // return a failed CompletableFuture with the Exception instead.
      throw new IllegalStateException("Client already closed");
    }
    return _client.sendAsync(req, HttpResponse.BodyHandlers.ofString());
  }

  // TODO expose on the connection
  public CompletableFuture<HttpResponse<Void>> streamRequest(HttpRequest req, Flow.Subscriber<? super String> subscription) {
    if (isClosed()) {
      // TODO: [DRV-174] do not throw the exception in the calling thread,
      // return a failed CompletableFuture with the Exception instead.
      throw new IllegalStateException("Client already closed");
    }
    return _client.sendAsync(req, HttpResponse.BodyHandlers.fromLineSubscriber(subscription));
  }


}
