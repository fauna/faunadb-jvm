package com.faunadb.common;

import com.faunadb.common.http.HttpClient;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

final class RefAwareHttpClient implements AutoCloseable {

  private static final int INITIAL_REF_COUNT = 1;

  private final AtomicInteger refCount = new AtomicInteger(INITIAL_REF_COUNT);
  private final HttpClient delegate;

  RefAwareHttpClient(HttpClient delegate) {
    this.delegate = delegate;
  }

  boolean retain() {
    return refCount.incrementAndGet() > INITIAL_REF_COUNT && !delegate.isClosed();
  }

  @Override
  public void close() {
    if (refCount.decrementAndGet() < INITIAL_REF_COUNT && !delegate.isClosed()) {
      try {
        delegate.close();
      } catch (IOException e) {
      }
    }
  }

  CompletableFuture<FullHttpResponse> sendRequest(FullHttpRequest request) {
    return delegate.sendRequest(request);
  }

}
