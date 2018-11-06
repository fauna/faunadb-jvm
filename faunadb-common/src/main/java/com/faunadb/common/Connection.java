package com.faunadb.common;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.faunadb.common.http.HttpClient;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.base64.Base64;
import io.netty.handler.codec.http.*;
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
import static io.netty.util.CharsetUtil.UTF_8;
import static java.lang.String.format;

/**
 * The HTTP Connection adapter for FaunaDB drivers.
 *
 * <p>Relies on <a href="https://netty.io/">Netty</a>
 * for the underlying implementation.</p>
 *
 * <p>The {@link Connection#close()} method must be called in order to
 * release {@link Connection} I/O resources</p>
 */
public final class Connection implements AutoCloseable {

  private static final int DEFAULT_CONNECTION_TIMEOUT_MS = 10000;
  private static final int DEFAULT_REQUEST_TIMEOUT_MS = 60000;
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
    private MetricRegistry metricRegistry;

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
     * @return a newly constructed {@link Connection} with its configuration based on
     * the settings of the {@link Builder} instance.
     */
    public Connection build() {
      MetricRegistry registry;
      if (metricRegistry == null)
        registry = new MetricRegistry();
      else
        registry = metricRegistry;

      URL root;
      if (faunaRoot == null) {
        root = FAUNA_ROOT;
      }
      else {
        root = faunaRoot;
      }

      HttpClient http = new HttpClient(root, DEFAULT_CONNECTION_TIMEOUT_MS, DEFAULT_REQUEST_TIMEOUT_MS);

      return new Connection(root, authToken, new RefAwareHttpClient(http), registry);
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

  private Connection(URL faunaRoot, String authToken, RefAwareHttpClient client, MetricRegistry registry) {
    this.faunaRoot = faunaRoot;
    this.authHeader = generateAuthHeader(authToken);
    this.client = client;
    this.registry = registry;
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
      return new Connection(faunaRoot, authToken, client, registry);
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
   * @return a {@link CompletableFuture} containing the HTTP Response.
   * @throws IOException if the HTTP request cannot be issued.
   */
  public CompletableFuture<FullHttpResponse> get(String path) throws IOException {
    FullHttpRequest request = newRequest(HttpMethod.GET, path);
    return performRequest(request);
  }

  /**
   * Issues a {@code GET} request with the provided request parameters.
   *
   * @param path   the relative path of the resource.
   * @param params a map containing the request parameters.
   * @return a {@code CompletableFuture} containing the HTTP response.
   * @throws IOException if the HTTP request cannot be issued.
   */
  public CompletableFuture<FullHttpResponse> get(String path, Map<String, List<String>> params) throws IOException {
    FullHttpRequest request = newRequest(HttpMethod.GET, path);
    fixRequestParameters(request, params);
    return performRequest(request);
  }

  /**
   * Issues a {@code POST} request with the provided JSON request body.
   *
   * @param path the relative path of the resource.
   * @param body the JSON tree that will be serialized into the request body.
   * @return a {@link CompletableFuture} containing the HTTP response.
   * @throws IOException if the HTTP request cannot be issued.
   */
  public CompletableFuture<FullHttpResponse> post(String path, JsonNode body) throws IOException {
    FullHttpRequest request = newRequest(HttpMethod.POST, path, body);
    return performRequest(request);
  }

  /**
   * Issues a {@code PUT} request with the provided JSON request body.
   *
   * @param path the relative path of the resource.
   * @param body the JSON tree that will be serialized into the request body.
   * @return a {@link CompletableFuture} containing the HTTP response.
   * @throws IOException if the HTTP request cannot be issued.
   */
  public CompletableFuture<FullHttpResponse> put(String path, JsonNode body) throws IOException {
    FullHttpRequest request = newRequest(HttpMethod.PUT, path, body);
    return performRequest(request);
  }

  /**
   * Issues a {@code PATCH} request with the provided JSON request body.
   *
   * @param path the relative path of the resource.
   * @param body the JSON tree that will be serialized into the request body.
   * @return a {@link CompletableFuture} containing the HTTP response.
   * @throws IOException if the HTTP request cannot be issued.
   */
  public CompletableFuture<FullHttpResponse> patch(String path, JsonNode body) throws IOException {
    FullHttpRequest request = newRequest(HttpMethod.PATCH, path, body);
    return performRequest(request);
  }

  private FullHttpRequest newRequest(HttpMethod method, String path) throws IOException {
    return new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, mkUrl(path));
  }

  private void fixRequestParameters(FullHttpRequest request, Map<String, List<String>> params) {
    QueryStringEncoder encoder = new QueryStringEncoder(request.uri());

    for (Map.Entry<String, List<String>> entries : params.entrySet()) {
      String k = entries.getKey();
      for (String v : entries.getValue())
        encoder.addParam(k, v);
    }

    request.setUri(encoder.toString());
  }

  private FullHttpRequest newRequest(HttpMethod method, String path, JsonNode body) throws IOException {
    FullHttpRequest request = newRequest(method, path);

    byte[] jsonBody = json.writeValueAsBytes(body);
    request.content().clear().writeBytes(jsonBody);

    request.headers().set(HttpHeaderNames.CONTENT_LENGTH, jsonBody.length);
    request.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=utf-8");

    return request;
  }

  private CompletableFuture<FullHttpResponse> performRequest(final FullHttpRequest request) {
    final Timer.Context ctx = registry.timer("fauna-request").time();
    final CompletableFuture<FullHttpResponse> rv = new CompletableFuture();

    request.headers().add("Authorization", authHeader);
    request.headers().set("X-FaunaDB-API-Version", "2.1");

    long time = txnTime.get();
    if (time > 0) {
      request.headers().set("X-Last-Seen-Txn", Long.toString(time));
    }

    request.retain();

    client.sendRequest(request).whenCompleteAsync((response, throwable) -> {

      ctx.stop();

      if (throwable != null) {
        logFailure(request, throwable);
        request.release();
        rv.completeExceptionally(throwable);
        return;
      }

      rv.complete(response);

      String txnTimeHeader = response.headers().get("X-Txn-Time");
      if (txnTimeHeader != null) {
        syncLastTxnTime(Long.valueOf(txnTimeHeader));
      }

      logSuccess(request, response);
      request.release();
    });

    return rv;
  }

  private String mkUrl(String path) throws MalformedURLException {
    return new URL(faunaRoot, path).toString();
  }

  private void logSuccess(FullHttpRequest request, FullHttpResponse response) {
    if (log.isDebugEnabled()) {
      String data = Optional.ofNullable(request.content().toString(UTF_8)).orElse("");
      String body = Optional.ofNullable(response.content().toString(UTF_8)).orElse("");
      String host = response.headers().get(X_FAUNADB_HOST, "Unknown");
      String build = response.headers().get(X_FAUNADB_BUILD, "Unknown");

      log.debug(
        format("Request: %s %s: [%s]. Response: Status=%d, Fauna Host: %s, Fauna Build: %s: %s",
          request.method(), request.uri(), data, response.status().code(), host, build, body));
    }
  }

  private void logFailure(FullHttpRequest request, Throwable ex) {
    log.info(
      format("Request: %s %s: %s. Failed: %s",
        request.method(), request.uri(), request.content().toString(UTF_8), ex.getMessage()), ex);
  }

  private static String generateAuthHeader(String authToken) {
    String token = authToken + ":";
    ByteBuf byteBuf = Unpooled.wrappedBuffer(token.getBytes(US_ASCII));
    ByteBuf enc = Base64.encode(byteBuf);
    String hdr = "Basic " + enc.toString(US_ASCII);
    enc.release();
    return hdr;
  }
}
