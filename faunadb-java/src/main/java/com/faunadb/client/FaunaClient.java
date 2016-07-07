package com.faunadb.client;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.faunadb.client.errors.*;
import com.faunadb.client.query.Expr;
import com.faunadb.client.types.Value;
import com.faunadb.common.Connection;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Response;

import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.TimeoutException;

/**
 * The Java native client for FaunaDB.
 * <p>
 * This client is asynchronous, so all methods that perform latent operations return a {@link ListenableFuture}.
 * <p>
 * Queries are constructed by using the static helpers in the {@link com.faunadb.client.query.Language} package.
 * <p>
 * <b>Example</b>:
 * <pre>{@code
 * import static com.faunadb.client.query.Language.*;
 * FaunaClient client = FaunaClient.builder().withSecret("someAuthToken").build();
 * client.query(Get(Ref("some/ref")));
 * }
 * </pre>
 *
 * @see com.faunadb.client.query.Language
 */
public class FaunaClient implements AutoCloseable {

  private static final Charset UTF8 = Charset.forName("UTF-8");

  /**
   * Creates a new {@link Builder}
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
     * Sets the FaunaDB endpoint url for the built {@link FaunaClient}.
     *
     * @param endpoint the root endpoint URL
     * @return this {@link Builder} object
     */
    public Builder withEndpoint(String endpoint) throws MalformedURLException {
      this.endpoint = new URL(endpoint);
      return this;
    }

    /**
     * Sets a {@link MetricRegistry} that the {@link FaunaClient} will use to register and track Connection-level
     * statistics.
     *
     * @param registry the MetricRegistry instance.
     * @return this {@link Builder} object
     */
    public Builder withMetrics(MetricRegistry registry) {
      this.registry = registry;
      return this;
    }

    /**
     * Sets a custom {@link AsyncHttpClient} implementation that the built {@link FaunaClient} will use.
     * This custom implementation can be provided to control the behavior of the underlying HTTP transport.
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
   * Creates a session client with the user secret provided. Queries submited to a session client will be
   * authenticated with the secret provided. A session client shares its parent's {@link Connection} instance.
   *
   * @param secret user secret for the session client
   * @return a new {@link FaunaClient}
   */
  public FaunaClient newSessionClient(String secret) {
    return new FaunaClient(connection.newSessionConnection(secret));
  }

  /**
   * Frees any resources held by the client. Also closes the underlying {@link Connection}.
   */
  @Override
  public void close() {
    connection.close();
  }

  /**
   * Issues a Query to FaunaDB.
   * <p>
   * Queries are modeled through the FaunaDB query language, represented by the helper functions in the
   * {@link com.faunadb.client.query.Language} class.
   * <p>
   * Responses are modeled as a general response tree. Each node is a {@link Value}, and
   * can be coerced to structured types through various methods on that class.
   *
   * @param expr The query expression to be sent to FaunaDB.
   * @return A {@link ListenableFuture} containing the root node of the Response tree.
   * @see Value
   * @see com.faunadb.client.query.Language
   */
  public ListenableFuture<Value> query(Expr expr) {
    JsonNode body = json.valueToTree(expr);
    try {
      return handleNetworkExceptions(Futures.transform(connection.post("/", body), new Function<Response, Value>() {
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

  /**
   * Issues multiple queries to FaunaDB.
   * <p>
   * These queries are sent to FaunaDB in a single request, and are evaluated. The list of response nodes is returned
   * in the same order as the issued queries.
   * <p>
   * See {@link FaunaClient#query(Expr)} for more information on the individual queries.
   *
   * @param exprs the list of query expressions to be sent to FaunaDB.
   * @return a {@link ListenableFuture} containing an ordered list of root response nodes.
   */
  public ListenableFuture<ImmutableList<Value>> query(List<? extends Expr> exprs) {
    JsonNode body = json.valueToTree(exprs);

    try {
      return handleNetworkExceptions(Futures.transform(connection.post("/", body), new Function<Response, ImmutableList<Value>>() {
        @Override
        public ImmutableList<Value> apply(Response resp) {
          try {
            handleQueryErrors(resp);
            JsonNode responseBody = parseResponseBody(resp);
            ArrayNode resources = ((ArrayNode) responseBody.get("resource"));
            ImmutableList.Builder<Value> responseNodeBuilder = ImmutableList.builder();

            for (JsonNode resource : resources) {
              responseNodeBuilder.add(json.treeToValue(resource, Value.class));
            }

            return responseNodeBuilder.build();
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
