package com.faunadb.client;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.faunadb.client.errors.*;
import com.faunadb.client.query.Expr;
import com.faunadb.client.streaming.BodyValueFlowProcessor;
import com.faunadb.client.streaming.EventField;
import com.faunadb.client.streaming.SnapshotEventFlowProcessor;
import com.faunadb.client.types.Field;
import com.faunadb.client.types.MetricsResponse;
import com.faunadb.client.types.Value;
import com.faunadb.common.Connection;
import com.faunadb.common.Connection.JvmDriver;
import com.faunadb.client.types.Value.NullV;

import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.faunadb.common.http.ResponseBodyStringProcessor;

import static com.faunadb.client.query.Language.Get;
import static com.faunadb.client.types.Codec.VALUE;

/**
 * The Java native client for FaunaDB.
 *
 * <p>The client is asynchronous. All methods that performs latent operations
 * return an instance of {@link CompletableFuture}.</p>
 *
 * <p>Queries are constructed by using the static methods in the
 * {@link com.faunadb.client.query.Language} class.</p>
 *
 * <b>Example</b>:
 *
 * <pre>{@code
 * import static com.faunadb.client.query.Language.*;
 *
 * FaunaClient client = FaunaClient.builder()
 *   .withSecret("someAuthToken")
 *   .build();
 *
 * client.query(
 *   Get(
 *     Ref(Collection("some_collection"), "123")
 *   )
 * );
 * }
 * </pre>
 *
 * @see com.faunadb.client.query.Language
 */
public class FaunaClient {

  /**
   * Creates a new {@link Builder}
   * @return {@link Builder}
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * A builder for creating an instance of {@link FaunaClient}
   */
  public static final class Builder {

    private String secret;
    private URL endpoint;
    private MetricRegistry registry;
    private Duration queryTimeout;
    private String userAgent;
    private boolean checkNewVersion = true;
    private Map<String, String> customHeaders;

    private Builder() {
    }

    /**
     * Sets the secret to be passed to FaunaDB as a authentication token
     *
     * @param secret the auth token secret
     * @return this {@link Builder} object
     */
    public Builder withSecret(String secret) {
      this.secret = secret;
      return this;
    }

    /**
     * Sets the FaunaDB endpoint url for the {@link FaunaClient} instance.
     *
     * @param endpoint the root endpoint URL
     * @return this {@link Builder} object
     * @throws MalformedURLException if the endpoint is invalid
     */
    public Builder withEndpoint(String endpoint) throws MalformedURLException {
      this.endpoint = new URL(endpoint);
      return this;
    }

    /**
     * Sets a {@link MetricRegistry} that the {@link FaunaClient} will use to register and track Connection-level
     * statistics.
     *
     * @param registry the {@link MetricRegistry} instance.
     * @return this {@link Builder} object
     */
    public Builder withMetrics(MetricRegistry registry) {
      this.registry = registry;
      return this;
    }

    /**
     * Sets a global timeout for all the Queries issued by this client.
     *
     * @param timeout the query timeout value. The timeout value has milliseconds precision.
     * @return this {@link Builder} object
     */
    public Builder withQueryTimeout(Duration timeout) {
      this.queryTimeout = timeout;
      return this;
    }

    /**
     * Sets the User-Agent header for all the Queries issued by this client.
     *
     * @param userAgent the userAgent value
     * @return this {@link Builder} object
     */
    public Builder withUserAgent(String userAgent) {
      this.userAgent = userAgent;
      return this;
    }

    /**
     * Sets the checkNewVersion variable for checking latets driver version
     *
     * @param checkNewVersion
     * @return this {@link Connection.Builder} object
     */
    public Builder withCheckNewVersion(boolean checkNewVersion) {
      this.checkNewVersion = checkNewVersion;
      return this;
    }

    /**
     * Sets user defined headers that will be sent with each http request to fauna db server
     * @param headers a map of key-value pairs
     * @return this {@link Connection.Builder} object
     */
    public Builder withCustomHeaders(Map<String, String> headers) {
      this.customHeaders = headers;
      return this;
    }

    /**
     * Returns a newly constructed {@link FaunaClient} with configuration based on the settings of this {@link Builder}.
     * @return {@link FaunaClient}
     */
    public FaunaClient build() {
      Connection.Builder builder = Connection.builder()
        .withAuthToken(secret)
        .withFaunaRoot(endpoint)
        .withQueryTimeout(queryTimeout)
        .withUserAgent(userAgent)
        .withJvmDriver(JvmDriver.JAVA)
        .withCheckNewDriverVersion(checkNewVersion);

      if (registry != null) builder.withMetrics(registry);
      if (customHeaders != null) builder.withCustomHeaders(customHeaders);

      return new FaunaClient(builder.build());
    }
  }

  private static final ObjectMapper json = new ObjectMapper().registerModule(new Jdk8Module());
  private final Connection connection;

  private FaunaClient(Connection connection) {
    this.connection = connection;
  }

  /**
   * Creates a session client with the user secret provided. Queries submitted to a session client will be
   * authenticated with the secret provided. A session client shares its parent's {@link Connection} instance.
   *
   * @param secret user secret for the session client
   * @return a new {@link FaunaClient}
   */
  public FaunaClient newSessionClient(String secret) {
    return new FaunaClient(connection.newSessionConnection(secret));
  }

  /**
   * Issues a Query to FaunaDB.
   * <p>
   * Queries are constructed by the helper methods in the {@link com.faunadb.client.query.Language} class.
   * <p>
   * Responses are represented as structured tree where each node is a {@link Value} instance.
   * {@link Value} instances can be converted to native types. See {@link Value} class for details.
   *
   * @param expr the query to be executed.
   * @return a {@link CompletableFuture} containing the root node of the response tree.
   * @see Value
   * @see com.faunadb.client.query.Language
   */
  public CompletableFuture<Value> query(Expr expr) {
    return query(expr, Optional.empty());
  }

  /**
   * Issues a Query to FaunaDB.
   * <p>
   * Queries are constructed by the helper methods in the {@link com.faunadb.client.query.Language} class.
   * <p>
   * Responses are represented as structured tree where each node is a {@link Value} instance.
   * {@link Value} instances can be converted to native types. See {@link Value} class for details.
   *
   * @param expr the query to be executed.
   * @param timeout the timeout for the current query. It replaces the timeout value set for this
   *                {@link FaunaClient} (if any), for the scope of this query. The timeout value
   *                has milliseconds precision.
   * @return a {@link CompletableFuture} containing the root node of the response tree.
   * @see Value
   * @see com.faunadb.client.query.Language
   */
  public CompletableFuture<Value> query(Expr expr, Duration timeout) {
    return query(expr, Optional.ofNullable(timeout));
  }

  /**
   * Issues a Query to FaunaDB.
   * <p>
   * Queries are constructed by the helper methods in the {@link com.faunadb.client.query.Language} class.
   * <p>
   * Responses are represented as structured tree where each node is a {@link Value} instance.
   * {@link Value} instances can be converted to native types. See {@link Value} class for details.
   *
   * @param expr the query to be executed.
   * @param timeout the timeout for the current query. It replaces the timeout value set for this
   *                {@link FaunaClient} (if any), for the scope of this query. The timeout value
   *                has milliseconds precision.
   * @return a {@link CompletableFuture} containing the root node of the response tree.
   * @see Value
   * @see com.faunadb.client.query.Language
   */
  public CompletableFuture<Value> query(Expr expr, Optional<Duration> timeout) {
    return performRequest(json.valueToTree(expr), timeout);
  }

  /**
   * Issues a Query to FaunaDB with extra information
   * <p>
   * Queries are constructed by the helper methods in the {@link com.faunadb.client.query.Language} class.
   * <p>
   * Responses are represented as structured tree where each node is a {@link Value} instance.
   * {@link Value} instances can be converted to native types. See {@link Value} class for details.
   *
   * @param expr the query to be executed.
   * @param timeout the timeout for the current query. It replaces the timeout value set for this
   *                {@link FaunaClient} (if any), for the scope of this query. The timeout value
   *                has milliseconds precision.
   * @return a {@link CompletableFuture} containing the root node of the response tree.
   * @see MetricsResponse
   * @see com.faunadb.client.query.Language
   */
  public CompletableFuture<MetricsResponse> queryWithMetrics(Expr expr, Optional<Duration> timeout) {
    return performRequestWithMetrics(json.valueToTree(expr), timeout);
  }

  /**
   * Issues multiple queries to FaunaDB.
   * <p>
   * These queries are sent to FaunaDB in a single request. A list containing all responses is returned
   * in the same order as the issued queries.
   * <p>
   *
   * @param exprs the list of queries to be sent to FaunaDB.
   * @return a {@link CompletableFuture} containing an ordered list of the query's responses.
   */
  public CompletableFuture<List<Value>> query(Expr... exprs) {
    return query(Arrays.asList(exprs));
  }

  /**
   * Issues multiple queries to FaunaDB.
   * <p>
   * These queries are sent to FaunaDB in a single request. A list containing all responses is returned
   * in the same order as the issued queries.
   * <p>
   *
   * @param exprs the list of queries to be sent to FaunaDB.
   * @return a {@link CompletableFuture} containing an ordered list of the query's responses.
   */
  public CompletableFuture<List<Value>> query(List<? extends Expr> exprs) {
    return query(exprs, Optional.empty());
  }

  /**
   * Issues multiple queries to FaunaDB.
   * <p>
   * These queries are sent to FaunaDB in a single request. A list containing all responses is returned
   * in the same order as the issued queries.
   * <p>
   *
   * @param exprs the list of queries to be sent to FaunaDB.
   * @param timeout the timeout for the current query. It replaces the timeout value set for this
   *                {@link FaunaClient} (if any), for the scope of this query. The timeout value
   *                has milliseconds precision.
   * @return a {@link CompletableFuture} containing an ordered list of the query's responses.
   */
  public CompletableFuture<List<Value>> query(List<? extends Expr> exprs, Duration timeout) {
    return query(exprs, Optional.ofNullable(timeout));
  }

  /**
   * Issues multiple queries to FaunaDB.
   * <p>
   * These queries are sent to FaunaDB in a single request. A list containing all responses is returned
   * in the same order as the issued queries.
   * <p>
   *
   * @param exprs the list of queries to be sent to FaunaDB.
   * @param timeout the timeout for the current query. It replaces the timeout value set for this
   *                {@link FaunaClient} (if any), for the scope of this query. The timeout value
   *                has milliseconds precision.
   * @return a {@link CompletableFuture} containing an ordered list of the query's responses.
   */
  public CompletableFuture<List<Value>> query(List<? extends Expr> exprs, Optional<Duration> timeout) {
    return performRequest(json.valueToTree(exprs), timeout).thenApply(result -> result.collect(Field.as(VALUE)));
  }

  /**
   * Sync the freshest timestamp seen by this client.
   * <p>
   * This has no effect if staler than currently stored timestamp.
   * <p>
   * WARNING: This should be used only when coordinating timestamps across
   *          multiple clients. Moving the timestamp arbitrarily forward into
   *          the future will cause transactions to stall.
   */
  public void syncLastTxnTime(long timestamp) {
    connection.syncLastTxnTime(timestamp);
  }

  /**
   * Get the freshest timestamp reported to this client.
   */
  public long getLastTxnTime() {
    return connection.getLastTxnTime();
  }

  private Value handleResponse(HttpResponse<String> response) {
    try {
      handleQueryErrors(response.statusCode(), response.body());
      JsonNode responseBody = parseResponseBody(response.body());
      JsonNode resource = responseBody.get("resource");

      if(resource == null) {
        throw new IllegalArgumentException("Invalid JSON.");
      }

      if(resource instanceof NullNode) {
        return NullV.NULL;
      }

      return json.treeToValue(resource, Value.class);
    } catch (JsonProcessingException | IllegalArgumentException ex) {
      throw new AssertionError(ex);
    }
  }

  private MetricsResponse handleResponseWithMetrics(HttpResponse<String> response) {
    Map<MetricsResponse.Metrics, String> metrics = new HashMap<>();
    MetricsResponse.Metrics.vals().forEach(m ->
        response.headers().firstValue(m.getMetric()).ifPresent(v -> metrics.put(m, v))
    );
    Value value = handleResponse(response);
    return MetricsResponse.of(value, metrics);
  }

  private CompletableFuture<Value> performRequest(JsonNode body, Optional<Duration> queryTimeout) {
    // TODO fixup
    return handleNetworkExceptions(connection.post("", body, queryTimeout, Optional.empty(), Optional.empty()).thenApply(this::handleResponse));
  }

  private CompletableFuture<MetricsResponse> performRequestWithMetrics(JsonNode body, Optional<Duration> queryTimeout) {
    // TODO fixup
    return handleNetworkExceptions(connection.post("", body, queryTimeout, Optional.empty(), Optional.empty()).thenApply(this::handleResponseWithMetrics));
  }

  /**
   * Creates a subscription to the result of the given read-only expression. When
   * executed, the expression must only perform reads and produce a single
   * streamable type, such as a reference or a version. Expressions that attempt
   * to perform writes or produce non-streamable types will result in an error.
   * Otherwise, any expression can be used to initiate a stream, including
   * user-defined function calls.
   *
   * @param expr the query to subscribe to.
   * @return a {@link CompletableFuture} containing a {@link java.util.concurrent.Flow.Publisher} of {@link Value}.
   * @see Value
   * @see com.faunadb.client.query.Language
   */
  public CompletableFuture<Flow.Publisher<Value>> stream(Expr expr) {
    return performStreamRequest(json.valueToTree(expr), List.of());
  }

  /**
   * Creates a subscription to the result of the given read-only expression. When
   * executed, the expression must only perform reads and produce a single
   * streamable type, such as a reference or a version. Expressions that attempt
   * to perform writes or produce non-streamable types will result in an error.
   * Otherwise, any expression can be used to initiate a stream, including
   * user-defined function calls.
   *
   * @param expr the query to subscribe to.
   * @param fields fields to opt-in on the events.
   * @param snapshot if true the second event will be a snapshot event of the target
   * @return a {@link CompletableFuture} containing a {@link java.util.concurrent.Flow.Publisher} of {@link Value}.
   * @see Value
   * @see com.faunadb.client.query.Language
   */
  public CompletableFuture<Flow.Publisher<Value>> stream(Expr expr, List<EventField> fields, boolean snapshot) {
    return performStreamRequest(json.valueToTree(expr), fields).thenApply( valuePublisher -> {
      if (snapshot) {
        Function<Expr, CompletableFuture<Value>> loadDocument = x -> query(Get(x));
        SnapshotEventFlowProcessor snapshotEventFlowProcessor = new SnapshotEventFlowProcessor(expr, loadDocument);
        valuePublisher.subscribe(snapshotEventFlowProcessor);
        return snapshotEventFlowProcessor;
      } else {
        return valuePublisher;
      }
    });
  }

  private CompletableFuture<Flow.Publisher<Value>> performStreamRequest(JsonNode body, List<EventField> fields) {
    Map<String, List<String>> params = Map.of("fields", fields.stream().map(EventField::value).collect(Collectors.toList()));
    try {
      return handleNetworkExceptions(
        connection.performStreamRequest("POST", "stream", body, params)
          .thenCompose(response -> {
            CompletableFuture<Flow.Publisher<Value>> publisher = new CompletableFuture<>();
            if (response.statusCode() < 300) {
              BodyValueFlowProcessor bodyValueFlowProcessor = new BodyValueFlowProcessor(json, connection);
              response.body().subscribe(bodyValueFlowProcessor);
              publisher.complete(bodyValueFlowProcessor);
            } else {
              ResponseBodyStringProcessor.consumeBody(response).thenCompose(bodyString -> {
                try {
                  // this always throws in the error case
                  handleQueryErrors(response.statusCode(), bodyString);
                } catch (Exception ex) {
                  publisher.completeExceptionally(ex);
                }
                return publisher;
              });
            }
            return publisher;
          }
        )
      );
    } catch (Exception ex) {
      CompletableFuture<Flow.Publisher<Value>> oops = new CompletableFuture<>();
      oops.completeExceptionally(ex);
      return oops;
    }
  }

  private void handleQueryErrors(int statusCode, String body) {
    if (statusCode >= 300) {
      try {
        List<HttpResponses.QueryError> parsedErrors = new ArrayList<>();

        ArrayNode errors = (ArrayNode) parseResponseBody(body).get("errors");
        if (errors != null) {
          for (JsonNode errorNode : errors) {
            parsedErrors.add(json.treeToValue(errorNode, HttpResponses.QueryError.class));
          }
        }

        HttpResponses.QueryErrorResponse errorResponse = HttpResponses.QueryErrorResponse.create(statusCode, parsedErrors);

        switch (statusCode) {
          case 400:
            throw new BadRequestException(errorResponse);
          case 401:
            throw new UnauthorizedException(errorResponse);
          case 403:
            throw new PermissionDeniedException(errorResponse);
          case 404:
            throw new NotFoundException(errorResponse);
          case 409:
            throw new TransactionContentionException(errorResponse);
          case 500:
            throw new InternalException(errorResponse);
          case 503:
            throw new UnavailableException(errorResponse);
          default:
            throw new UnknownException(errorResponse);
        }
      } catch (JsonProcessingException | IllegalArgumentException ex) {
        if (statusCode == 503) {
          throw new UnavailableException("Service Unavailable: Unparseable response.", ex);
        } else {
          throw new UnknownException("Unparseable service " + statusCode + " response.", ex);
        }
      }
    }
  }

  private <V> CompletableFuture<V> handleNetworkExceptions(CompletableFuture<V> f) {
    return f.whenComplete((v, ex) -> {
      if (ex instanceof ConnectException || ex instanceof TimeoutException) {
        throw new UnavailableException(ex.getMessage(), ex);
      }
      if (ex instanceof CompletionException && ex.getCause() instanceof IOException && ex.getMessage().contains("header parser received no bytes")) {
        throw new UnavailableException(ex.getMessage(), ex);
      }
      if (ex instanceof CompletionException && ex.getCause() instanceof IOException && ex.getMessage().contains("too many concurrent streams")) {
        throw new BadRequestException("the maximum number of streams has been reached for this client");
      }
    });
  }

  private JsonNode parseResponseBody(String responseBody) throws JsonProcessingException, IllegalArgumentException {
    JsonNode body = json.readTree(responseBody);
    if (body == null) {
      throw new IllegalArgumentException("Invalid JSON.");
    } else {
      return body;
    }
  }

}
