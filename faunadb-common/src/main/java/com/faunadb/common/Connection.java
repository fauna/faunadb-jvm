package com.faunadb.common;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.faunadb.common.http.JavaHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOError;
import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

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
public class Connection implements AutoCloseable {

  private static final String API_VERSION = "4";
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

  public enum JvmDriver {
    JAVA("Java"),
    SCALA("Scala");

    private String stringValue;

    JvmDriver(String stringValue) {
      this.stringValue = stringValue;
    }

    @Override public String toString() {
      return this.stringValue;
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
    private long lastSeenTxn;
    private JavaHttpClient client;
    private JvmDriver jvmDriver;
    private Optional<Duration> queryTimeout = Optional.empty();

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
     * Sets the Fauna driver to use for the connection.
     *
     * @param jvmDriver the {@link JvmDriver} to use for this connection.
     * @return this {@link Builder} object
     */
    public Builder withJvmDriver(JvmDriver jvmDriver) {
      this.jvmDriver = jvmDriver;
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
     * Sets the client to use for the connection.
     *
     * @param client the {@link JavaHttpClient} to use for this connection.
     * @return this {@link Builder} object
     */
    public Builder withHttpClient(JavaHttpClient client) {
      this.client = client;
      return this;
    }

    /**
     * Sets the global query timeout for this connection.
     *
     * @param timeout the query timeout value
     * @return this {@link Builder} object
     */
    public Builder withQueryTimeout(Duration timeout) {
      this.queryTimeout = Optional.ofNullable(timeout);
      return this;
    }

    /**
     * @return a newly constructed {@link Connection} with its configuration based on
     * the settings of the {@link Builder} instance.
     */
    public Connection build() {
      MetricRegistry registry;
      registry = Objects.requireNonNullElseGet(metricRegistry, MetricRegistry::new);

      URL root;
      root = Objects.requireNonNullElseGet(faunaRoot, () -> FAUNA_ROOT);

      JavaHttpClient http;
      http = Objects.requireNonNullElseGet(client, () -> new JavaHttpClient(DEFAULT_CONNECTION_TIMEOUT_MS));

      return new Connection(root, authToken, http, registry, jvmDriver, lastSeenTxn, queryTimeout);
    }
  }

  private static final String X_FAUNADB_HOST = "X-FaunaDB-Host";
  private static final String X_FAUNADB_BUILD = "X-FaunaDB-Build";
  private static final String X_FAUNA_DRIVER = "X-Fauna-Driver";
  private static final String X_QUERY_TIMEOUT = "X-Query-Timeout";

  private final URL faunaRoot;
  private final String authHeader;
  private final JvmDriver jvmDriver;
  private final JavaHttpClient client;
  private final MetricRegistry registry;
  private final Optional<Duration> connectionQueryTimeout;

  private final Logger log = LoggerFactory.getLogger(getClass());
  private final ObjectMapper json = new ObjectMapper();
  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final AtomicLong txnTime = new AtomicLong(0L);

  private Connection(URL faunaRoot, String authToken, JavaHttpClient client, MetricRegistry registry, JvmDriver jvmDriver, long lastSeenTxn, Optional<Duration> connectionQueryTimeout) {
    this.faunaRoot = faunaRoot;
    this.authHeader = generateAuthHeader(authToken);
    this.client = client;
    this.registry = registry;
    this.jvmDriver = jvmDriver;
    this.txnTime.set(lastSeenTxn);
    this.connectionQueryTimeout = connectionQueryTimeout;
  }

  /**
   * Creates a new {@link Connection} sharing its underneath I/O resources. Queries submitted to a
   * session connection will be authenticated with the token provided. The {@link #close()} method
   * must be called before releasing the connection.
   *
   * @param authToken the token or key to be used to authenticate requests to the new {@link Connection}
   * @return a new {@link Connection}
   */
  // TODO: [DRV-174] Update JavaDoc to reflect new close method behaviour
  public Connection newSessionConnection(String authToken) {
    if (isClosed()) {
      throw new IllegalStateException("Connection already closed");
    }

    return new Connection(faunaRoot, authToken, client, registry, jvmDriver, getLastTxnTime(), connectionQueryTimeout) {
      @Override
      public void close() {
        // DO NOTHING
      }
    };
  }

  /**
   * Releases any resources being held by the {@link Connection} instance.
   */
  @Override
  public void close() {
    if (closed.compareAndSet(false, true)) {
      client.close();
    }
  }

  /**
   * Verifies if the connection still accepts new requests.
   *
   * @return true if closed, false if not
   * @see #close()
   */
  public boolean isClosed() {
    return closed.get();
  }

  /**
   * Get the freshest timestamp reported to this client.
   */
  public long getLastTxnTime() {
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
  public void syncLastTxnTime(long newTxnTime) {
    for (;;) {
      long oldTxnTime = getLastTxnTime();

      if (oldTxnTime >= newTxnTime || txnTime.compareAndSet(oldTxnTime, newTxnTime)) {
        return;
      }
    }
  }

  /**
   * Issues a {@code GET} request with no parameters.
   *
   * @param path the relative path of the resource.
   * @param queryTimeout the query timeout for the current request.
   * @return a {@link CompletableFuture} containing the HTTP Response.
   * @throws IOException if the HTTP request cannot be issued.
   */
  public CompletableFuture<HttpResponse<String>> get(String path, Optional<Duration> queryTimeout) throws Exception {
    return performRequest("GET", path, Optional.empty(), Map.of(), queryTimeout);
  }

  /**
   * Issues a {@code GET} request with the provided request parameters.
   *
   * @param path   the relative path of the resource.
   * @param params a map containing the request parameters.
   * @param queryTimeout the query timeout for the current request.
   * @return a {@code CompletableFuture} containing the HTTP response.
   * @throws IOException if the HTTP request cannot be issued.
   */
  public CompletableFuture<HttpResponse<String>> get(String path, Map<String, String> params, Optional<Duration> queryTimeout) throws Exception {
    return performRequest("GET", path, Optional.empty(), params, queryTimeout);
  }

  /**
   * Issues a {@code POST} request with the provided JSON request body.
   *
   * @param path the relative path of the resource.
   * @param body the JSON tree that will be serialized into the request body.
   * @param queryTimeout the query timeout for the current request.
   * @return a {@link CompletableFuture} containing the HTTP response.
   * @throws IOException if the HTTP request cannot be issued.
   */
  public CompletableFuture<HttpResponse<String>> post(String path, JsonNode body, Optional<Duration> queryTimeout) throws Exception {
    return performRequest("POST", path, Optional.of(body), Map.of(), queryTimeout);
  }

  /**
   * Issues a {@code PUT} request with the provided JSON request body.
   *
   * @param path the relative path of the resource.
   * @param body the JSON tree that will be serialized into the request body.
   * @param queryTimeout the query timeout for the current request.
   * @return a {@link CompletableFuture} containing the HTTP response.
   * @throws IOException if the HTTP request cannot be issued.
   */
  public CompletableFuture<HttpResponse<String>> put(String path, JsonNode body, Optional<Duration> queryTimeout) throws Exception {
    return performRequest("PUT", path, Optional.of(body), Map.of(), queryTimeout);
  }

  /**
   * Issues a {@code PATCH} request with the provided JSON request body.
   *
   * @param path the relative path of the resource.
   * @param body the JSON tree that will be serialized into the request body.
   * @param queryTimeout the query timeout for the current request.
   * @return a {@link CompletableFuture} containing the HTTP response.
   * @throws IOException if the HTTP request cannot be issued.
   */
  public CompletableFuture<HttpResponse<String>> patch(String path, JsonNode body, Optional<Duration> queryTimeout) throws Exception {
    return performRequest("PATCH", path, Optional.of(body), Map.of(), queryTimeout);
  }

  public static URI appendUri(URI oldUri, String queryKey, String queryValue) throws URISyntaxException {
    String urlEncodedKey = URLEncoder.encode(queryKey, StandardCharsets.UTF_8);
    String urlEncodedValue = URLEncoder.encode(queryValue, StandardCharsets.UTF_8);
    String query = urlEncodedKey + "=" + urlEncodedValue;
    return new URI(oldUri.getScheme(), oldUri.getAuthority(), oldUri.getPath(),
            oldUri.getQuery() == null ? query : oldUri.getQuery() + "&" + query, oldUri.getFragment());
  }

  private CompletableFuture<HttpResponse<String>> performRequest(String httpMethod, String path, Optional<JsonNode> body,
                                                                 Map<String, String> params, final Optional<Duration> requestQueryTimeout) throws Exception {
    final Timer.Context ctx = registry.timer("fauna-request").time();
    final CompletableFuture<HttpResponse<String>> rv = new CompletableFuture<>();

    URI requestUri = URI.create(mkUrl(path));

    // Encode all query parameters
    for (Map.Entry<String,String> entry : params.entrySet()) {
      requestUri = appendUri(requestUri, entry.getKey(), entry.getValue());
    }


    HttpRequest.BodyPublisher bodyPublisher = HttpRequest.BodyPublishers.noBody();
    if (body.isPresent()) {
      byte[] jsonBody = json.writeValueAsBytes(body.get());
      bodyPublisher = HttpRequest.BodyPublishers.ofByteArray(jsonBody);
    };

    // If a query timeout has been given for the current request,
    // override the one from the Connection if any
    Optional<Duration> queryTimeout = requestQueryTimeout.or(() -> connectionQueryTimeout);

    Optional<Long> lastTxnTime = (getLastTxnTime() > 0) ? Optional.of(getLastTxnTime()) : Optional.empty();

    HttpRequest.Builder requestBuilder =
      HttpRequest.newBuilder()
        .uri(requestUri)
        .version(HttpClient.Version.HTTP_1_1)
        .timeout(Duration.ofMillis(DEFAULT_REQUEST_TIMEOUT_MS))
        .method(httpMethod, bodyPublisher)
        .headers(
          "Authorization", authHeader,
          "X-FaunaDB-API-Version", API_VERSION,
          "User-agent", "Fauna JVM Http Client",
          X_FAUNA_DRIVER, jvmDriver.toString(),
          "Content-type", "application/json; charset=utf-8"
        );

    queryTimeout.ifPresent( timeout ->
      requestBuilder.header(X_QUERY_TIMEOUT, String.valueOf(timeout.toMillis()))
    );

    lastTxnTime.ifPresent( time ->
      requestBuilder.header("X-Last-Seen-Txn", Long.toString(time))
    );

    HttpRequest request = requestBuilder.build();

    client.sendRequest(request).whenCompleteAsync((response, throwable) -> {
      ctx.stop();

      if (throwable != null) {
        logFailure(request, throwable);
        rv.completeExceptionally(throwable);
        return;
      }

      Optional<String> txnTimeHeader = response.headers().firstValue("X-Txn-Time");
      txnTimeHeader.ifPresent(s -> syncLastTxnTime(Long.parseLong(s)));

      logSuccess(request, response);

      rv.complete(response);
    });

    return rv;
  }

  private String mkUrl(String path) throws MalformedURLException {
    return new URL(faunaRoot, path).toString();
  }

  private void logSuccess(HttpRequest request, HttpResponse<String> response) {
    if (log.isDebugEnabled()) {
      String data = request.bodyPublisher().map(Object::toString).orElse("NoBody");
      String body = Optional.ofNullable(response.body()).orElse("");
      String host = response.headers().firstValue(X_FAUNADB_HOST).orElse("Unknown");
      String build = response.headers().firstValue(X_FAUNADB_BUILD).orElse("Unknown");

      log.debug(
        format("Request: %s %s: [%s]. Response: Status=%d, Fauna Host: %s, Fauna Build: %s: %s",
          request.method(), request.uri(), data, response.statusCode(), host, build, body));
    }
  }

  private void logFailure(HttpRequest request, Throwable ex) {
    log.info(
      format("Request: %s %s: %s. Failed: %s",
        request.method(), request.uri(), request.bodyPublisher().map(Object::toString).orElse("NoBody").toString(), ex.getMessage()), ex);
  }

  private static String generateAuthHeader(String authToken) {
    return "Bearer " + authToken;
  }

}
