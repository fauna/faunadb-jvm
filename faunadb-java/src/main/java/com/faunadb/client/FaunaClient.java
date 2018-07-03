package com.faunadb.client;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.faunadb.client.errors.*;
import com.faunadb.client.query.Expr;
import com.faunadb.client.types.Field;
import com.faunadb.client.types.Value;
import com.faunadb.common.Connection;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.Response;

import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static com.faunadb.client.types.Codec.VALUE;
import static com.google.common.util.concurrent.Futures.transform;

/**
 * The Java native client for FaunaDB.
 *
 * <p>The client is asynchronous. All methods that performs latent operations
 * return an instance of {@link ListenableFuture}.</p>
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
 *     Ref(Class("some_class"), "123")
 *   )
 * );
 * }
 * </pre>
 *
 * @see com.faunadb.client.query.Language
 */
public class FaunaClient implements AutoCloseable {

  private static final String UTF8 = "UTF-8";

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
    private AsyncHttpClient httpClient;

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
     * Sets a custom {@link AsyncHttpClient} for the {@link FaunaClient} instance.
     * A custom implementation can be provided to control the behavior of the underlying HTTP transport.
     *
     * @param httpClient the custom {@link AsyncHttpClient} instance
     * @return this {@link Builder} object
     */
    public Builder withHttpClient(AsyncHttpClient httpClient) {
      this.httpClient = httpClient;
      return this;
    }

    /**
     * Returns a newly constructed {@link FaunaClient} with configuration based on the settings of this {@link Builder}.
     * @return {@link FaunaClient}
     */
    public FaunaClient build() {
      Connection.Builder builder = Connection.builder()
        .withAuthToken(secret)
        .withFaunaRoot(endpoint);

      if (registry != null) builder.withMetrics(registry);
      if (httpClient != null) builder.withHttpClient(httpClient);

      return new FaunaClient(builder.build());
    }
  }

  private final ObjectMapper json = new ObjectMapper().registerModule(new GuavaModule());
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
   * @return a {@link ListenableFuture} containing the root node of the response tree.
   * @see Value
   * @see com.faunadb.client.query.Language
   */
  public ListenableFuture<Value> query(Expr expr) {
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
   * @return a {@link ListenableFuture} containing an ordered list of the query's responses.
   */
  public ListenableFuture<ImmutableList<Value>> query(List<? extends Expr> exprs) {
    return transform(performRequest(json.valueToTree(exprs)), new Function<Value, ImmutableList<Value>>() {
      @Override
      public ImmutableList<Value> apply(Value result) {
        return result.collect(Field.as(VALUE));
      }
    });
  }

  private ListenableFuture<Value> performRequest(JsonNode body) {
    try {
      return handleNetworkExceptions(transform(connection.post("", body), new Function<Response, Value>() {
        @Override
        public Value apply(Response response) {
          try {
            handleQueryErrors(response);

            JsonNode responseBody = parseResponseBody(response);
            JsonNode resource = responseBody.get("resource");
            return json.treeToValue(resource, Value.class);
          } catch (IOException ex) {
            throw new AssertionError(ex);
          }
        }
      }));
    } catch (IOException ex) {
      return Futures.immediateFailedFuture(ex);
    }
  }

  private void handleQueryErrors(Response response) throws FaunaException {
    int status = response.getStatusCode();
    if (status >= 300) {
      try {
        ArrayNode errors = (ArrayNode) parseResponseBody(response).get("errors");
        ImmutableList.Builder<HttpResponses.QueryError> errorBuilder = ImmutableList.builder();

        for (JsonNode errorNode : errors) {
          errorBuilder.add(json.treeToValue(errorNode, HttpResponses.QueryError.class));
        }

        HttpResponses.QueryErrorResponse errorResponse = HttpResponses.QueryErrorResponse.create(status, errorBuilder.build());

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

  private <V> ListenableFuture<V> handleNetworkExceptions(ListenableFuture<V> f) {
    ListenableFuture<V> f1 = Futures.catching(f, ConnectException.class, new Function<ConnectException, V>() {
      @Override
      public V apply(ConnectException input) {
        throw new UnavailableException(input.getMessage());
      }
    });

    return Futures.catching(f1, TimeoutException.class, new Function<TimeoutException, V>() {
      @Override
      public V apply(TimeoutException input) {
        throw new UnavailableException(input.getMessage());
      }
    });
  }

  private JsonNode parseResponseBody(Response response) throws IOException {
    return json.readTree(response.getResponseBody(UTF8));
  }
}
