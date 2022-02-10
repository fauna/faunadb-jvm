package com.faunadb.common;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.faunadb.common.http.DriverVersionChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOError;
import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * The HTTP Connection adapter for FaunaDB drivers.
 *
 * <p>Relies on {@link java.net.http.HttpClient}
 * for the underlying implementation.</p>
 */
public class Connection {

  private static final String API_VERSION = "4";
  private static final Duration DEFAULT_CONNECTION_TIMEOUT = Duration.ofSeconds(10);
  private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(60);
  private static final String DEFAULT_USER_AGENT = "Fauna JVM Http Client";
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

  private static class RuntimeEnvironmentHeader {
    private String runtime;
    private String driverVersion;
    private String os;
    private String env;

    RuntimeEnvironmentHeader(JvmDriver jvmDriver, String scalaVersion, boolean checkNewVersion) {
      this.driverVersion = Connection.class.getPackage().getSpecificationVersion();
      this.os = System.getProperty("os.name");
      this.env = this.getRuntimeEnv();
      this.runtime = String.format("java-%s", System.getProperty("java.version"));

      if (jvmDriver == JvmDriver.SCALA) {
        this.runtime = String.format("%s,scala-%s", this.runtime, scalaVersion);
      }

      if (checkNewVersion && !DriverVersionChecker.isAlreadyChecked()) {
        DriverVersionChecker.checkLatestVersion();
      }
    }

    @Override public String toString() {
      return String.format(
        "driver=jvm-%s; runtime=%s; env=%s; os=%s",
        this.driverVersion,
        this.runtime,
        this.env,
        this.os
      ).toLowerCase();
    }

    private String getRuntimeEnv() {
      if (System.getenv("NETLIFY_IMAGES_CDN_DOMAIN") != null) {
        return "Netlify";
      }

      if (System.getenv("VERCEL") != null) {
        return "Vercel";
      }

      if (System.getenv("PATH") != null && System.getenv("PATH").contains(".heroku")) {
        return "Heroku";
      }

      if (System.getenv("AWS_LAMBDA_FUNCTION_VERSION") != null) {
        return "AWS Lambda";
      }

      if (System.getenv("_") != null && System.getenv("_").contains("google")) {
        return "GCP Cloud Functions";
      }

      if (System.getenv("GOOGLE_CLOUD_PROJECT") != null) {
        return "GCP Compute Instances";
      }

      if (System.getenv("ORYX_ENV_TYPE") != null && System.getenv("WEBSITE_INSTANCE_ID") != null && System.getenv("ORYX_ENV_TYPE") == "AppService") {
        return "Azure Compute";
      }

      return "Unknown";
    }
  
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
    private HttpClient client;
    private JvmDriver jvmDriver;
    private String scalaVersion;
    private Optional<Duration> queryTimeout = Optional.empty();
    private Optional<String> userAgent = Optional.empty();
    private boolean checkNewDriverVersion = true;
    private Map<String, String> customHeaders;

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
     * Sets the Scala version to use for a requests header
     *
     * @param scalaVersion
     * @return this {@link Builder} object
     */
    public Builder withScalaVersion(String scalaVersion) {
      this.scalaVersion = scalaVersion;
      return this;
    }

    /**
     * Sets the checkNewVersion variable for checking latets driver version
     *
     * @param checkNewVersion
     * @return this {@link Builder} object
     */
    public Builder withCheckNewDriverVersion(boolean checkNewVersion) {
      this.checkNewDriverVersion = checkNewVersion;
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
     * @param client the {@link HttpClient} to use for this connection.
     * @return this {@link Builder} object
     */
    public Builder withHttpClient(HttpClient client) {
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
     * Sets the User-Agent header for this connection.
     *
     * @param userAgent the query timeout value
     * @return this {@link Builder} object
     */
    public Builder withUserAgent(String userAgent) {
      this.userAgent = Optional.ofNullable(userAgent);
      return this;
    }

    public Builder withCustomHeaders(Map<String, String> headers) {
      this.customHeaders = headers;
      return this;
    }

    /**
     * @return a newly constructed {@link Connection} with its configuration based on
     * the settings of the {@link Builder} instance.
     */
    public Connection build() {
      System.setProperty("jdk.httpclient.keepalive.timeout", "4");
      MetricRegistry registry;
      registry = Objects.requireNonNullElseGet(metricRegistry, MetricRegistry::new);

      URL root;
      root = Objects.requireNonNullElseGet(faunaRoot, () -> FAUNA_ROOT);

      HttpClient http;
      http = Objects.requireNonNullElseGet(client, () ->
        // TODO: [DRV-169] allow users to override default executor
        HttpClient.newBuilder()
          .connectTimeout(DEFAULT_CONNECTION_TIMEOUT)
          .build()
      );

      String connectionUserAgent = userAgent.orElse(DEFAULT_USER_AGENT);
      String runtimeEnvironmentHeader = new RuntimeEnvironmentHeader(jvmDriver, scalaVersion, checkNewDriverVersion).toString();

      return new Connection(root, authToken, http, registry, runtimeEnvironmentHeader, lastSeenTxn, queryTimeout, connectionUserAgent, customHeaders);
    }
  }

  private static final String X_FAUNADB_HOST = "X-FaunaDB-Host";
  private static final String X_FAUNADB_BUILD = "X-FaunaDB-Build";
  private static final String X_DRIVER_ENV = "X-Driver-Env";
  private static final String X_QUERY_TIMEOUT = "X-Query-Timeout";
  private static final String X_LAST_SEEN_TXN = "X-Last-Seen-Txn";
  private static final String X_FAUNADB_API_VERSION = "X-FaunaDB-API-Version";
  private static final String USER_AGENT = "User-Agent";

  private final URL faunaRoot;
  private final String authHeader;
  private final String runtimeEnvironmentHeader;
  private HttpClient client;
  private final MetricRegistry registry;
  private final Optional<Duration> defaultQueryTimeout;
  private final String userAgent;
  private final Map<String, String> customHeaders;

  private final Logger log = LoggerFactory.getLogger(getClass());
  private final ObjectMapper json = new ObjectMapper();
  private final AtomicLong txnTime = new AtomicLong(0L);

  private Connection(URL faunaRoot, String authToken, HttpClient client, MetricRegistry registry, String runtimeEnvironmentHeader, long lastSeenTxn, Optional<Duration> defaultQueryTimeout, String userAgent, Map<String, String> customHeaders) {
    this.faunaRoot = faunaRoot;
    this.authHeader = generateAuthHeader(authToken);
    this.runtimeEnvironmentHeader = runtimeEnvironmentHeader;
    this.client = client;
    this.registry = registry;
    this.txnTime.set(lastSeenTxn);
    this.defaultQueryTimeout = defaultQueryTimeout;
    this.userAgent = userAgent;
    this.customHeaders = customHeaders;
  }

  /**
   * Creates a new {@link Connection} sharing its underneath I/O resources. Queries submitted to a
   * session connection will be authenticated with the token provided.
   *
   * @param authToken the token or key to be used to authenticate requests to the new {@link Connection}
   * @return a new {@link Connection}
   */
  public Connection newSessionConnection(String authToken) {
    return new Connection(faunaRoot, authToken, client, registry, runtimeEnvironmentHeader, getLastTxnTime(), defaultQueryTimeout, userAgent, customHeaders);
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
   */
  public CompletableFuture<HttpResponse<String>> get(String path, Optional<Duration> queryTimeout) {
    return performRequest("GET", path, Optional.empty(), Map.of(), queryTimeout);
  }

  /**
   * Issues a {@code GET} request with the provided request parameters.
   *
   * @param path   the relative path of the resource.
   * @param params a map containing the request parameters.
   * @param queryTimeout the query timeout for the current request.
   * @return a {@code CompletableFuture} containing the HTTP response.
   */
  public CompletableFuture<HttpResponse<String>> get(String path, Map<String, List<String>> params, Optional<Duration> queryTimeout) {
    return performRequest("GET", path, Optional.empty(), params, queryTimeout);
  }

  /**
   * Issues a {@code POST} request with the provided JSON request body.
   *
   * @param path the relative path of the resource.
   * @param body the JSON tree that will be serialized into the request body.
   * @param queryTimeout the query timeout for the current request.
   * @return a {@link CompletableFuture} containing the HTTP response.
   */
  public CompletableFuture<HttpResponse<String>> post(String path, JsonNode body, Optional<Duration> queryTimeout) {
    return performRequest("POST", path, Optional.of(body), Map.of(), queryTimeout);
  }

  /**
   * Issues a {@code PUT} request with the provided JSON request body.
   *
   * @param path the relative path of the resource.
   * @param body the JSON tree that will be serialized into the request body.
   * @param queryTimeout the query timeout for the current request.
   * @return a {@link CompletableFuture} containing the HTTP response.
   */
  public CompletableFuture<HttpResponse<String>> put(String path, JsonNode body, Optional<Duration> queryTimeout) {
    return performRequest("PUT", path, Optional.of(body), Map.of(), queryTimeout);
  }

  /**
   * Issues a {@code PATCH} request with the provided JSON request body.
   *
   * @param path the relative path of the resource.
   * @param body the JSON tree that will be serialized into the request body.
   * @param queryTimeout the query timeout for the current request.
   * @return a {@link CompletableFuture} containing the HTTP response.
   */
  public CompletableFuture<HttpResponse<String>> patch(String path, JsonNode body, Optional<Duration> queryTimeout) {
    return performRequest("PATCH", path, Optional.of(body), Map.of(), queryTimeout);
  }

  private static URI appendUri(URI oldUri, String queryKey, List<String> queryValues) throws URISyntaxException {
    String urlEncodedKey = URLEncoder.encode(queryKey, StandardCharsets.UTF_8);
    String urlEncodedValue = queryValues.stream()
            .map(u -> URLEncoder.encode(u, StandardCharsets.UTF_8))
            .collect(Collectors.joining(","));
    String query = urlEncodedKey + "=" + urlEncodedValue;
    return new URI(oldUri.getScheme(), oldUri.getAuthority(), oldUri.getPath(),
            oldUri.getQuery() == null ? query : oldUri.getQuery() + "&" + query, oldUri.getFragment());
  }

  private CompletableFuture<HttpResponse<String>> performRequest(String httpMethod, String path, Optional<JsonNode> body,
                                                                 Map<String, List<String>> params, final Optional<Duration> requestQueryTimeout) {
    final Timer.Context ctx = registry.timer("fauna-request").time();
    final CompletableFuture<HttpResponse<String>> rv = new CompletableFuture<>();
    HttpRequest request;
    try {
      request = makeHttpRequest(httpMethod, path, body, params, requestQueryTimeout, HttpClient.Version.HTTP_1_1);
    } catch (IllegalArgumentException| MalformedURLException | URISyntaxException | JsonProcessingException ex) {
      rv.completeExceptionally(ex);
      return rv;
    }
    sendRequest(request).whenCompleteAsync((response, throwable) -> {
      ctx.stop();
      if (throwable != null) {
        logFailure(request, throwable);
        rv.completeExceptionally(throwable);
        return;
      }

      Optional<String> txnTimeHeader = response.headers().firstValue("x-txn-time");
      txnTimeHeader.ifPresent(s -> syncLastTxnTime(Long.parseLong(s)));

      logSuccess(request, response);

      rv.complete(response);
    });

    return rv;
  }

  private CompletableFuture<HttpResponse<String>> sendRequest(HttpRequest req) {
    return client.sendAsync(req, HttpResponse.BodyHandlers.ofString());
  }

  public CompletableFuture<HttpResponse<Flow.Publisher<List<ByteBuffer>>>> streamRequest(HttpRequest req) {
    return client.sendAsync(req, HttpResponse.BodyHandlers.ofPublisher());
  }

  public CompletableFuture<HttpResponse<Flow.Publisher<List<ByteBuffer>>>> performStreamRequest(String httpMethod, String path, JsonNode body,
                                                                                                Map<String, List<String>> params) {
    final Timer.Context ctx = registry.timer("fauna-request").time();
    final CompletableFuture<HttpResponse<Flow.Publisher<List<ByteBuffer>>>> rv = new CompletableFuture<>();
    HttpRequest request;
    try {
      request = makeHttpRequest(httpMethod, path, Optional.of(body), params, Optional.empty(), HttpClient.Version.HTTP_2);
    } catch (MalformedURLException | URISyntaxException | JsonProcessingException ex) {
      rv.completeExceptionally(ex);
      return rv;
    }
    streamRequest(request).whenCompleteAsync((response, throwable) -> {
      ctx.stop();
      if (throwable != null) {
        logFailure(request, throwable);
        rv.completeExceptionally(throwable);
        return;
      }

      Optional<String> txnTimeHeader = response.headers().firstValue("x-txn-time");
      txnTimeHeader.ifPresent(s -> syncLastTxnTime(Long.parseLong(s)));

      rv.complete(response);
    });

    return rv;
  }

  private HttpRequest makeHttpRequest(String httpMethod, String path, Optional<JsonNode> body, Map<String, List<String>> params,
                                      Optional<Duration> requestQueryTimeout, HttpClient.Version httpVersion) throws MalformedURLException, URISyntaxException, JsonProcessingException {
    URI requestUri = URI.create(mkUrl(path));

    // Encode all query parameters
    for (Map.Entry<String, List<String>> entry : params.entrySet()) {
      List<String> values = entry.getValue();
      if (!values.isEmpty()) {
        requestUri = appendUri(requestUri, entry.getKey(), values);
      }
    }

    HttpRequest.BodyPublisher bodyPublisher = HttpRequest.BodyPublishers.noBody();
    if (body.isPresent()) {
      byte[] jsonBody = json.writeValueAsBytes(body.get());
      bodyPublisher = HttpRequest.BodyPublishers.ofByteArray(jsonBody);
    }

    // If a query timeout has been given for the current request,
    // override the one from the Connection if any
    // or use a default value
    Duration queryTimeout = requestQueryTimeout.or(() -> defaultQueryTimeout).orElse(DEFAULT_REQUEST_TIMEOUT);

    Optional<Long> lastTxnTime = (getLastTxnTime() > 0) ? Optional.of(getLastTxnTime()) : Optional.empty();

    HttpRequest.Builder requestBuilder =
      HttpRequest.newBuilder()
        .uri(requestUri)
        .version(httpVersion)
        .timeout(queryTimeout)
        .method(httpMethod, bodyPublisher)
        .headers(
          mkHeaders(queryTimeout)
        );

    lastTxnTime.ifPresent(time -> requestBuilder.header(X_LAST_SEEN_TXN, Long.toString(time)));

    return requestBuilder.build();
  }

  private String[] mkHeaders(Duration queryTimeout) {
    Map<String, String> defaultHeaders = Map.of(
            "Authorization", authHeader,
            X_FAUNADB_API_VERSION, API_VERSION,
            USER_AGENT, userAgent,
            X_QUERY_TIMEOUT, String.valueOf(queryTimeout.toMillis()),
            X_DRIVER_ENV, runtimeEnvironmentHeader,
            "Content-type", "application/json; charset=utf-8"
    );

    Map<String, String> combinedHeaders = new HashMap<>(defaultHeaders);
    if (customHeaders != null) {
      for (Map.Entry<String, String> entry : customHeaders.entrySet()) {
        combinedHeaders.put(entry.getKey(), entry.getValue());
      }
    }

    String[] headersAsArray = new String[combinedHeaders.size() * 2];
    int index = 0;
    for (Map.Entry<String, String> entry : combinedHeaders.entrySet()) {
      headersAsArray[index] = entry.getKey();
      headersAsArray[index + 1] = entry.getValue();
      index += 2;
    }
    return headersAsArray;
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
        request.method(), request.uri(), request.bodyPublisher().map(Object::toString).orElse("NoBody"), ex.getMessage()), ex);
  }

  private static String generateAuthHeader(String authToken) {
    return "Bearer " + authToken;
  }
}
