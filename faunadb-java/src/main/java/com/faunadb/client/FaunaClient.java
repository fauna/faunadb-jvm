package com.faunadb.client;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.faunadb.client.errors.*;
import com.faunadb.client.query.Expr;
import com.faunadb.client.types.Field;
import com.faunadb.client.types.Value;
import com.faunadb.common.Connection;
import com.faunadb.common.Connection.JvmDriver;
import io.netty.buffer.ByteBufInputStream;
import io.netty.handler.codec.http.FullHttpResponse;

import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import static com.faunadb.client.types.Codec.VALUE;

/**
 * The Java native client for FaunaDB.
 *
 * <p>The client is asynchronous. All methods that performs latent operations
 * return an instance of {@link CompletableFuture}.</p>
 *
 * <p>The {@link FaunaClient#close()} method must be called in order to
 * release the {@link FaunaClient} I/O resources.</p>
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
public class FaunaClient implements AutoCloseable {

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
     * Returns a newly constructed {@link FaunaClient} with configuration based on the settings of this {@link Builder}.
     * @return {@link FaunaClient}
     */
    public FaunaClient build() {
      Connection.Builder builder = Connection.builder()
        .withAuthToken(secret)
        .withFaunaRoot(endpoint)
        .withJvmDriver(JvmDriver.JAVA);

      if (registry != null) builder.withMetrics(registry);

      return new FaunaClient(builder.build());
    }
  }

  private final ObjectMapper json = new ObjectMapper().registerModule(new Jdk8Module());
  private final Connection connection;

  private FaunaClient(Connection connection) {
    this.connection = connection;
  }

  /**
   * Creates a session client with the user secret provided. Queries submitted to a session client will be
   * authenticated with the secret provided. A session client shares its parent's {@link Connection} instance
   * and must be closed after used.
   *
   * @param secret user secret for the session client
   * @return a new {@link FaunaClient}
   */
  public FaunaClient newSessionClient(String secret) {
    return new FaunaClient(connection.newSessionConnection(secret));
  }

  /**
   * Releases any resources being held by the {@link FaunaClient} instance.
   */
  @Override
  public void close() {
    connection.close();
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
    return performRequest(json.valueToTree(expr));
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
      return performRequest(json.valueToTree(exprs)).thenApply(result -> result.collect(Field.as(VALUE)));
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

  private Value handleResponse(FullHttpResponse response) {
    try {
      handleQueryErrors(response);
      JsonNode responseBody = parseResponseBody(response);
      JsonNode resource = responseBody.get("resource");
      return json.treeToValue(resource, Value.class);
    } catch (IOException ex) {
      throw new AssertionError(ex);
    } finally {
      response.release();
    }
  }

  private CompletableFuture<Value> performRequest(JsonNode body) {
    try {
        return handleNetworkExceptions(connection.post("", body).thenApply(this::handleResponse));
    } catch (IOException ex) {
        CompletableFuture<Value> oops = new CompletableFuture<>();
        oops.completeExceptionally(ex);
        return oops;
    }
  }

  private void handleQueryErrors(FullHttpResponse response) throws FaunaException {
    int status = response.status().code();
    if (status >= 300) {
      try {
        ArrayNode errors = (ArrayNode) parseResponseBody(response).get("errors");
        List<HttpResponses.QueryError> errorBuilder = new ArrayList<>();

        for (JsonNode errorNode : errors) {
          errorBuilder.add(json.treeToValue(errorNode, HttpResponses.QueryError.class));
        }

        HttpResponses.QueryErrorResponse errorResponse = HttpResponses.QueryErrorResponse.create(status, errorBuilder);

        switch (status) {
          case 400:
            throw new BadRequestException(errorResponse);
          case 401:
            throw new UnauthorizedException(errorResponse);
          case 403:
            throw new PermissionDeniedException(errorResponse);
          case 404:
            throw new NotFoundException(errorResponse);
          case 500:
            throw new InternalException(errorResponse);
          case 503:
            throw new UnavailableException(errorResponse);
          default:
            throw new UnknownException(errorResponse);
        }
      } catch (IOException ex) {
        switch (status) {
          case 503:
            throw new UnavailableException("Service Unavailable: Unparseable response.");
          default:
            throw new UnknownException("Unparseable service " + status + "response.");
        }
      }
    }
  }

  private <V> CompletableFuture<V> handleNetworkExceptions(CompletableFuture<V> f) {
      return f.whenComplete((v, ex) -> {
              if (ex instanceof ConnectException ||
                         ex instanceof TimeoutException) {
                  throw new UnavailableException(ex.getMessage());
              }
          });
  }

  private JsonNode parseResponseBody(FullHttpResponse response) throws IOException {
    JsonNode body = json.readTree(new ByteBufInputStream(response.content()));
    if (body == null) {
      throw new IOException("Invalid JSON.");
    } else {
      return body;
    }
  }

}
