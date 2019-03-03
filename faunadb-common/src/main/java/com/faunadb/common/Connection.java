package com.faunadb.common;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.base64.Base64;
import io.netty.handler.codec.http.HttpHeaderNames;
import org.asynchttpclient.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOError;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static io.netty.util.CharsetUtil.US_ASCII;
import static java.lang.String.format;

/**
 * The HTTP Connection adapter for FaunaDB drivers.
 *
 * <p>Relies on <a href="https://github.com/AsyncHttpClient/async-http-client">async-http-client</a>
 * for the underlying implementation.</p>
 *
 * <p>The {@link Connection#close()} method must be called in order to
 * release {@link Connection} I/O resources</p>
 */
public final class Connection implements AutoCloseable {

  private static final int DEFAULT_CONNECTION_TIMEOUT_MS = 10000;
  private static final int DEFAULT_REQUEST_TIMEOUT_MS = 60000;
  private static final int DEFAULT_IDLE_TIMEOUT_MS = 4750;
  private static final int DEFAULT_MAX_RETRIES = 0;
  private static final URL FAUNA_ROOT;

  static {
    try {
      FAUNA_ROOT = new URL("https://db.fauna.com");
    } catch (MalformedURLException e) {
      throw new IOError(e); // won't happen
    }
  }

  /**
   * Returns a new {@link Builder} instance.
   *
   * @return a new {@link Builder}
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * A builder for the {@link Connection} instance. Use the {@link Connection#builder} method to create
   * an instance of the {@link Builder} class.
   */
  public static class Builder {

    private URL faunaRoot;
    private String authToken;
    private AsyncHttpClient client;
    private MetricRegistry metricRegistry;
    private long lastSeenTxn;

    private Builder() {
    }

    /**
     * Sets the FaunaDB root URL for the {@link Connection} instance.
     *
     * @param root the root URL, as a RFC 2396 formatted string. Example: https://db.fauna.com
     * @return this {@link Builder} object
     * @throws MalformedURLException if a malformed url is provided
     */
    public Builder withFaunaRoot(String root) throws MalformedURLException {
      this.faunaRoot = new URL(root);
      return this;
    }

    /**
     * Sets the FaunaDB root URL for the {@link Connection} instance.
     *
     * @param root the root URL
     * @return this {@link Builder} object
     */
    public Builder withFaunaRoot(URL root) {
      this.faunaRoot = root;
      return this;
    }

    /**
     * Sets the authentication token or key for the {@link Connection} instance.
     *
     * @param token the auth token or key
     * @return this {@link Builder} object
     */
    public Builder withAuthToken(String token) {
      this.authToken = token;
      return this;
    }

    /**
     * Sets a custom {@link AsyncHttpClient} implementation for the {@link Connection} instance.
     * A custom implementation can be provided to control the behavior of the underlying HTTP transport.
     *
     * @param client the custom {@link AsyncHttpClient} instance
     * @return this {@link Builder} object
     */
    public Builder withHttpClient(AsyncHttpClient client) {
      this.client = client;
      return this;
    }

    /**
     * Sets a {@link MetricRegistry} for the {@link Connection} instance.
     * The {@link MetricRegistry} will be used to track connection level statistics.
     *
     * @param registry the {@link MetricRegistry} instance.
     * @return this {@link Builder} object
     */
    public Builder withMetrics(MetricRegistry registry) {
      this.metricRegistry = registry;
      return this;
    }

    /**
     * Sets the last seen transaction time for the connection.
     *
     * @param txnTime the last seen transaction time in microseconds.
     * @return this {@link Builder} object
     */
    public Builder withLastSeenTxn(long txnTime) {
      this.lastSeenTxn = txnTime;
      return this;
    }

    /**
     * @return a newly constructed {@link Connection} with its configuration based on
     * the settings of the {@link Builder} instance.
     */
    public Connection build() {
      MetricRegistry registry;
      if (metricRegistry == null)
        registry = new MetricRegistry();
      else
        registry = metricRegistry;

      AsyncHttpClient httpClient;
      if (client == null) {
        httpClient = new DefaultAsyncHttpClient(
          new DefaultAsyncHttpClientConfig.Builder()
            .setConnectTimeout(DEFAULT_CONNECTION_TIMEOUT_MS)
            .setRequestTimeout(DEFAULT_REQUEST_TIMEOUT_MS)
            .setPooledConnectionIdleTimeout(DEFAULT_IDLE_TIMEOUT_MS)
            .setMaxRequestRetry(DEFAULT_MAX_RETRIES)
            .build()
        );
      } else {
        httpClient = client;
      }

      URL root;
      if (faunaRoot == null)
        root = FAUNA_ROOT;
      else
        root = faunaRoot;

      return new Connection(root, authToken, new RefAwareHttpClient(httpClient), registry, lastSeenTxn);
    }
  }

  private static final String X_FAUNADB_HOST = "X-FaunaDB-Host";
  private static final String X_FAUNADB_BUILD = "X-FaunaDB-Build";

  private final URL faunaRoot;
  private final String authHeader;
  private final RefAwareHttpClient client;
  private final MetricRegistry registry;

  private final Logger log = LoggerFactory.getLogger(getClass());
  private final ObjectMapper json = new ObjectMapper();
  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final AtomicLong txnTime = new AtomicLong(0L);

  private Connection(URL faunaRoot, String authToken, RefAwareHttpClient client, MetricRegistry registry, long lastSeenTxn) {
    this.faunaRoot = faunaRoot;
    this.authHeader = generateAuthHeader(authToken);
    this.client = client;
    this.registry = registry;
    txnTime.set(lastSeenTxn);
  }

  /**
   * Creates a new {@link Connection} sharing its underneath I/O resources. Queries submitted to a
   * session connection will be authenticated with the token provided. The {@link #close()} method
   * must be called before releasing the connection.
   *
   * @param authToken the token or key to be used to authenticate requests to the new {@link Connection}
   * @return a new {@link Connection}
   */
  public Connection newSessionConnection(String authToken) {
    if (client.retain())
      return new Connection(faunaRoot, authToken, client, registry, getLastTxnTime());
    else
      throw new IllegalStateException("Can not create a session connection from a closed http connection");
  }

  /**
   * Releases any resources being held by the {@link Connection} instance.
   */
  @Override
  public void close() {
    if (closed.compareAndSet(false, true))
      client.close();
  }

  /**
   * Get the freshest timestamp reported to this client.
   */
  public Long getLastTxnTime() {
    return txnTime.get();
  }

  /**
   * Sync the freshest timestamp seen by this client.
   *
   * This has no effect if more stale than the currently stored timestamp.
   * WARNING: This should be used only when coordinating timestamps across
   *          multiple clients. Moving the timestamp arbitrarily forward into
   *          the future will cause transactions to stall.
   */
  public void syncLastTxnTime(Long newTxnTime) {
    boolean cas;
    do {
      long oldTxnTime = txnTime.get();

      if (oldTxnTime < newTxnTime) {
        cas = txnTime.compareAndSet(oldTxnTime, newTxnTime);
      } else {
        // Another query advanced the txnTime past this one.
        break;
      }
    } while (!cas);
  }

  /**
   * Issues a {@code GET} request with no parameters.
   *
   * @param path the relative path of the resource.
   * @return a {@link ListenableFuture} containing the HTTP Response.
   * @throws IOException if the HTTP request cannot be issued.
   */
  public CompletableFuture<Response> get(String path) throws IOException {
    Request request = new RequestBuilder("GET")
      .setUrl(mkUrl(path))
      .build();

    return performRequest(request);
  }

  /**
   * Issues a {@code GET} request with the provided request parameters.
   *
   * @param path   the relative path of the resource.
   * @param params a map containing the request parameters.
   * @return a {@code ListenableFuture} containing the HTTP response.
   * @throws IOException if the HTTP request cannot be issued.
   */
  public CompletableFuture<Response> get(String path, Map<String, List<String>> params) throws IOException {
    Request request = new RequestBuilder("GET")
      .setUrl(mkUrl(path))
      .setQueryParams(params)
      .build();

    return performRequest(request);
  }

  /**
   * Issues a {@code POST} request with the provided JSON request body.
   *
   * @param path the relative path of the resource.
   * @param body the JSON tree that will be serialized into the request body.
   * @return a {@link ListenableFuture} containing the HTTP response.
   * @throws IOException if the HTTP request cannot be issued.
   */
  public CompletableFuture<Response> post(String path, JsonNode body) throws IOException {
    Request request = new RequestBuilder("POST")
      .setUrl(mkUrl(path))
      .setBody(json.writeValueAsString(body))
      .setHeader(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=utf-8")
      .build();

    return performRequest(request);
  }

  /**
   * Issues a {@code PUT} request with the provided JSON request body.
   *
   * @param path the relative path of the resource.
   * @param body the JSON tree that will be serialized into the request body.
   * @return a {@link ListenableFuture} containing the HTTP response.
   * @throws IOException if the HTTP request cannot be issued.
   */
  public CompletableFuture<Response> put(String path, JsonNode body) throws IOException {
    Request request = new RequestBuilder("PUT")
      .setUrl(mkUrl(path))
      .setBody(json.writeValueAsString(body))
      .setHeader(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=utf-8")
      .build();

    return performRequest(request);
  }

  /**
   * Issues a {@code PATCH} request with the provided JSON request body.
   *
   * @param path the relative path of the resource.
   * @param body the JSON tree that will be serialized into the request body.
   * @return a {@link ListenableFuture} containing the HTTP response.
   * @throws IOException if the HTTP request cannot be issued.
   */
  public CompletableFuture<Response> patch(String path, JsonNode body) throws IOException {
    Request request = new RequestBuilder("PATCH")
      .setUrl(mkUrl(path))
      .setBody(json.writeValueAsString(body))
      .setHeader(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=utf-8")
      .build();

    return performRequest(request);
  }

  private CompletableFuture<Response> performRequest(final Request request) {
    final Timer.Context ctx = registry.timer("fauna-request").time();
    final CompletableFuture<Response> rv = new CompletableFuture<>();

    BoundRequestBuilder req = client.prepareRequest(request)
      .addHeader("Authorization", authHeader)
      .setHeader("X-FaunaDB-API-Version", "2.1");

    long time = getLastTxnTime();
    if (time > 0) {
      req = req.setHeader("X-Last-Seen-Txn", Long.toString(time));
    }

    req.execute(new AsyncCompletionHandler<Response>() {
        @Override
        public void onThrowable(Throwable t) {
          ctx.stop();
          rv.completeExceptionally(t);
          logFailure(request, t);
        }

        @Override
        public Response onCompleted(Response response) {
          ctx.stop();

          String txnTimeHeader = response.getHeader("X-Txn-Time");
          if (txnTimeHeader != null) {
            syncLastTxnTime(Long.valueOf(txnTimeHeader));
          }

          logSuccess(request, response);
          rv.complete(response);
          return response;
        }
      });

    return rv;
  }

  private String mkUrl(String path) throws MalformedURLException {
    return new URL(faunaRoot, path).toString();
  }

  private void logSuccess(Request request, Response response) {
    if (log.isDebugEnabled()) {
      String data = Optional.ofNullable(request.getStringData()).orElse("");
      String host = Optional.ofNullable(response.getHeader(X_FAUNADB_HOST)).orElse("Unknown");
      String build = Optional.ofNullable(response.getHeader(X_FAUNADB_BUILD)).orElse("Unknown");
      String body = Optional.ofNullable(getResponseBody(response)).orElse("");

      log.debug(
        format("Request: %s %s: %s. Response: Status=%d, Fauna Host: %s, Fauna Build: %s: %s",
          request.getMethod(), request.getUrl(), data, response.getStatusCode(), host, build, body));
    }
  }

  private String getResponseBody(Response response) {
    return response.getResponseBody();
  }

  private void logFailure(Request request, Throwable ex) {
    String data = Optional.ofNullable(request.getStringData()).orElse("");

    log.info(
      format("Request: %s %s: %s. Failed: %s",
        request.getMethod(), request.getUrl(), data, ex.getMessage()), ex);
  }

  private static String generateAuthHeader(String authToken) {
    String token = authToken + ":";
    ByteBuf byteBuf = Unpooled.wrappedBuffer(token.getBytes(US_ASCII));
    ByteBuf enc = Base64.encode(byteBuf, false); // Must not break encoded lines
    String hdr = "Basic " + enc.toString(US_ASCII);
    enc.release();
    return hdr;
  }
}
