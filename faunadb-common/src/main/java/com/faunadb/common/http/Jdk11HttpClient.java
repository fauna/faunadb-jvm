package com.faunadb.common.http;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.*;

public class Jdk11HttpClient {

    private HttpClient _client;
    private ExecutorService _executor;
    private long _requestTimeout;

    public Jdk11HttpClient(int connectionTimeout, int requestTimeout) {
        this._requestTimeout = requestTimeout;
        this._executor = Executors.newCachedThreadPool();
        this._client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(connectionTimeout))
                .executor(_executor)
                .build();
    }

    public Void close() {
        _executor.shutdownNow();
        _client = null;
        return null;
    }

    public CompletableFuture<HttpResponse<String>> sendRequest(HttpRequest req) {
        if (_executor.isShutdown()) {
            throw new IllegalStateException("Client already closed");
        }
        return _client.sendAsync(req, HttpResponse.BodyHandlers.ofString());
    }

    // TODO expose on the connection
    public CompletableFuture<HttpResponse<Void>> streamRequest(HttpRequest req, Flow.Subscriber<? super String> subscription) {
        if (_executor.isShutdown()) {
            throw new IllegalStateException("Client already closed");
        }
        return _client.sendAsync(req, HttpResponse.BodyHandlers.fromLineSubscriber(subscription));
    }


}
