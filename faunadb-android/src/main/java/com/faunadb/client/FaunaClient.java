package com.faunadb.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.faunadb.client.errors.*;
import com.faunadb.client.query.Expr;
import com.faunadb.client.types.Field;
import com.faunadb.client.types.Value;

import java.io.IOError;
import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;

import okhttp3.*;

import static com.faunadb.client.types.Codec.VALUE;

/**
 * The Java native client for FaunaDB.
 * <p>
 * This client is asynchronous, so all methods that perform latent operations return a {@link CompletableFuture}.
 * <p>
 * Queries are constructed by using the static helpers in the {@link com.faunadb.client.query.Language} package.
 * <p>
 * <b>Example</b>:
 * <pre>{@code
 * import static com.faunadb.client.query.Language.*;
 * FaunaClient client = FaunaClient.builder().withSecret("someAuthToken").build();
 * client.query(Get(Ref(Class("some_class"), "ref")));
 * }
 * </pre>
 *
 * @see com.faunadb.client.query.Language
 */
public class FaunaClient {

  private static final int INITIAL_REF_COUNT = 0;
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

  private final AtomicInteger refCount;

  /**
   * Creates a new {@link Builder}
   *
   * @return a new {@link Builder}
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
    private OkHttpClient httpClient;

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
     * @throws java.net.MalformedURLException when an invalid string is passed
     */
    public Builder withEndpoint(String endpoint) throws MalformedURLException {
      this.endpoint = new URL(endpoint);
      return this;
    }

    /**
     * Sets a custom {@link OkHttpClient} implementation that the built {@link FaunaClient} will use.
     * This custom implementation can be provided to control the behavior of the underlying HTTP transport.
     *
     * @param httpClient the custom {@link OkHttpClient} instance
     * @return this {@link Builder} object
     */
    public Builder withHttpClient(OkHttpClient httpClient) {
      this.httpClient = httpClient;
      return this;
    }

    /**
     * Returns a newly constructed {@link FaunaClient} with configuration based on the settings of this {@link Builder}.
     *
     * @return a new {@link FaunaClient}
     */
    public FaunaClient build() {
      URL faunaEndpoint = FAUNA_ROOT;
      if (endpoint != null)
        faunaEndpoint = endpoint;

      if (httpClient == null) {
        return new FaunaClient(new OkHttpClient.Builder()
                .connectTimeout(DEFAULT_CONNECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .readTimeout(DEFAULT_REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .writeTimeout(DEFAULT_REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .retryOnConnectionFailure(true)
                .build(), faunaEndpoint, secret);
      }

      return new FaunaClient(httpClient, faunaEndpoint, secret);
    }
  }

  private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();
  private final OkHttpClient client;
  private final URL endpoint;
  private final String authHeader;

  private FaunaClient(OkHttpClient client, URL endpoint, String secret) {
    this(new AtomicInteger(INITIAL_REF_COUNT), client, endpoint, secret);
  }

  private FaunaClient(AtomicInteger refCount, OkHttpClient client, URL endpoint, String secret) {
    this.refCount = refCount;
    this.client = client;
    this.endpoint = endpoint;
    this.authHeader = Credentials.basic(secret, "");

    this.refCount.incrementAndGet();
  }

  /**
   * Creates a session client with the user secret provided. Queries submited to a session client will be
   * authenticated with the secret provided.
   *
   * @param secret user secret for the session client
   * @return a new {@link FaunaClient}
   */
  public FaunaClient newSessionClient(String secret) {
    if (refCount.get() > 0)
      return new FaunaClient(refCount, client, endpoint, secret);
    else
      throw new IllegalStateException("Can not create a session connection from a closed http connection");
  }

  /**
   * Frees any resources held by the client. Also closes the underlying {@link OkHttpClient}.
   *
   */
  public void close() {
    if (refCount.decrementAndGet() == 0) {
      client.dispatcher().executorService().shutdown();
      client.connectionPool().evictAll();
    }
  }

  /**
   * Issues a Query to FaunaDB.
   * <p>
   * Queries are modeled through the FaunaDB query language, represented by the helper functions in the
   * {@link com.faunadb.client.query.Language} class.
   * <p>
   * Responses are modeled as a general response tree. Each node is a {@link Value}, and
   * can be converted to structured types through various methods on that class.
   *
   * @param expr The query expression to be sent to FaunaDB.
   * @return A {@link CompletableFuture} containing the root node of the Response tree.
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
   * These queries are sent to FaunaDB in a single request, and are evaluated. The list of response nodes is returned
   * in the same order as the issued queries.
   * <p>
   * See {@link FaunaClient#query(Expr)} for more information on the individual queries.
   *
   * @param exprs the list of query expressions to be sent to FaunaDB.
   * @return a {@link CompletableFuture} containing an ordered list of root response nodes.
   */
  public CompletableFuture<List<Value>> query(List<? extends Expr> exprs) {
      return performRequest(json.valueToTree(exprs)).thenApply(new Function<Value, List<Value>>() {
              public List<Value> apply(Value result) {
                  return result.collect(Field.as(VALUE));
              }
          });
  }

  private CompletableFuture<Value> performRequest(JsonNode body) {
    try {
      Request request = new Request.Builder()
              .url(endpoint)
              .post(RequestBody.create(MediaType.parse("application/json"), json.writeValueAsString(body)))
              .addHeader("Authorization", authHeader)
              .addHeader("X-FaunaDB-API-Version", "2.1")
              .build();

      final CompletableFuture<Value> rv = new CompletableFuture();

      client.newCall(request).enqueue(new Callback() {
        @Override
        public void onFailure(Call call, IOException ex) {
          rv.completeExceptionally(ex);
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException {
          try {
            handleQueryErrors(response);

            JsonNode responseBody = parseResponseBody(response);
            JsonNode resource = responseBody.get("resource");
            rv.complete(json.treeToValue(resource, Value.class));
          } catch (Exception ex) {
            rv.completeExceptionally(ex);
          }
        }
      });

      return handleNetworkExceptions(rv);
    } catch (Exception ex) {
        CompletableFuture<Value> oops = new CompletableFuture();
        oops.completeExceptionally(ex);
        return oops;
    }
  }

  private void handleQueryErrors(Response response) throws FaunaException {
    int status = response.code();
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
    return f.whenComplete(new BiConsumer<V, Throwable>() {
      public void accept(V v, Throwable ex) {
        if (ex == null) {
          return;
        } else if (ex instanceof ConnectException ||
                   ex instanceof TimeoutException) {
          throw new UnavailableException(ex.getMessage());
        }
      }
    });
  }

  private JsonNode parseResponseBody(Response response) throws IOException {
    return json.readTree(response.body().string());
  }
}
