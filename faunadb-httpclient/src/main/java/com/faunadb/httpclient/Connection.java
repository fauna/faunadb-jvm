package com.faunadb.httpclient;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.ning.http.client.*;
import com.ning.http.util.Base64;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * The HTTP Connection adapter for FaunaDB clients.
 *
 * <p>Relies on <a href="https://github.com/AsyncHttpClient/async-http-client">async-http-client</a>
 * for the underlying implementation.
 */

public class Connection {
  static final int DEFAULT_CONNECTION_TIMEOUT_MS = 5000;
  static final int DEFAULT_REQUEST_TIMEOUT_MS = 30000;

  /**
   * Returns a new {@link Connection.Builder}.
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * A builder for creating an instance of {@link Connection}. Use {@link Connection#builder} to obtain
   * an instance of this builder.
   */
  public static class Builder {
    private URL faunaRoot;
    private String authToken;
    private AsyncHttpClient client;
    private MetricRegistry metricRegistry;

    private Builder() {
    }

    /**
     * Sets the FaunaDB root URL for the built {@link Connection}.
     *
     * @param root the root URL, as a RFC 2396 formatted string. Example: https://rest.faunadb.com
     * @return this {@link Builder} object
     * @throws MalformedURLException if {@code root} is not RFC 2396.
     */
    public Builder withFaunaRoot(String root) throws MalformedURLException {
      this.faunaRoot = new URL(root);
      return this;
    }

    /**
     * Sets the FaunaDB root URL for the built {@link Connection}.
     *
     * @param root the root URL
     * @return this {@link Builder} object
     */
    public Builder withFaunaRoot(URL root) {
      this.faunaRoot = root;
      return this;
    }

    /**
     * Sets the Auth Token that the built {@link Connection} will provide to FaunaDB. This must be provided in order
     * for client to authenticate with FaunaDB.
     *
     * @param token the auth token.
     * @return this {@link Builder} object
     */
    public Builder withAuthToken(String token) {
      this.authToken = token;
      return this;
    }

    /**
     * Sets a custom {@link AsyncHttpClient} implementation that the built {@link Connection} will use. This custom implementation
     * can be provided to control the behavior of the underlying HTTP transport.
     *
     * @param client the custom {@link AsyncHttpClient} instance
     * @return this {@link Builder} object
     */
    public Builder withHttpClient(AsyncHttpClient client) {
      this.client = client;
      return this;
    }

    /**
     * Sets a {@link MetricRegistry} that the {@link Connection} will use to register and track Connection-level statistics.
     * @param registry the MetricRegistry instance.
     * @return this {@link Builder} object
     *
     */
    public Builder withMetrics(MetricRegistry registry) {
      this.metricRegistry = registry;
      return this;
    }

    /**
     * Returns a newly constructed {@link Connection} with configuration based on the settings of this {@link Builder}.
     *
     * @throws UnsupportedEncodingException if the system does not support ASCII encoding for the Connection.
     * @throws MalformedURLException if the default FaunaDB URL cannot be parsed.
     */
    public Connection build() throws UnsupportedEncodingException, MalformedURLException {
      MetricRegistry r;
      if (metricRegistry == null)
        r = new MetricRegistry();
      else
        r = metricRegistry;

      AsyncHttpClient c;
      if (client == null) {
        AsyncHttpClientConfig config = new AsyncHttpClientConfig.Builder()
          .setConnectTimeout(10000)
          .setRequestTimeout(60000)
          .setMaxRequestRetry(0)
          .build();
        c = new AsyncHttpClient(config);
      } else
        c = client;

      URL root;
      if (faunaRoot == null)
        root = new URL("https://rest.faunadb.com");
      else
        root = faunaRoot;

      return new Connection(root, authToken, c, r);
    }
  }

  private static String XFaunaDBHost = "X-FaunaDB-Host";
  private static String XFaunaDBBuild = "X-FaunaDB-Build";

  private final URL faunaRoot;
  private final String authToken;
  private final String authHeader;
  private final AsyncHttpClient client;
  private final MetricRegistry registry;

  private final Logger log = LoggerFactory.getLogger(getClass());
  private final ObjectMapper json = new ObjectMapper();

  Connection(URL faunaRoot, String authToken, AsyncHttpClient client, MetricRegistry registry) throws UnsupportedEncodingException {
    this.faunaRoot = faunaRoot;
    this.authToken = authToken;
    this.authHeader = generateAuthHeader(authToken);
    this.client = client;
    this.registry = registry;
  }

  /**
   * Issues a {@code GET} request with no parameters.
   *
   * @param path the relative path of the resource.
   * @return a {@code ListenableFuture} containing the HTTP Response.
   * @throws IOException if the HTTP request cannot be issued.
   */
  public ListenableFuture<Response> get(String path) throws IOException {
    Request request = new RequestBuilder("GET")
      .setUrl(mkUrl(path))
      .build();

    return performRequest(request);
  }

  /**
   * Issues a {@code GET} request with the provided request parameters.
   *
   * @param path the relative path of the resource.
   * @param params a map containing the request parameters.
   * @return a {@code ListenableFuture} containing the HTTP response.
   * @throws IOException if the HTTP request cannot be issued.
   */
  public ListenableFuture<Response> get(String path, Map<String, List<String>> params) throws IOException {
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
  public ListenableFuture<Response> post(String path, JsonNode body) throws IOException {
    Request request = new RequestBuilder("POST")
      .setUrl(mkUrl(path))
      .setBody(json.writeValueAsString(body))
      .setHeader(HttpHeaders.Names.CONTENT_TYPE, "application/json; charset=utf-8")
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
  public ListenableFuture<Response> put(String path, JsonNode body) throws IOException {
    Request request = new RequestBuilder("PUT")
      .setUrl(mkUrl(path))
      .setBody(json.writeValueAsString(body))
      .setHeader(HttpHeaders.Names.CONTENT_TYPE, "application/json; charset=utf-8")
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
  public ListenableFuture<Response> patch(String path, JsonNode body) throws IOException {
    Request request = new RequestBuilder("PATCH")
      .setUrl(mkUrl(path))
      .setBody(json.writeValueAsString(body))
      .setHeader(HttpHeaders.Names.CONTENT_TYPE, "application/json; charset=utf-8")
      .build();

    return performRequest(request);
  }

  /**
   * Releases any resources being held by the HTTP client. Also closes the underlying
   * {@link AsyncHttpClient}.
   */
  public void close() {
    client.close();
  }

  private ListenableFuture<Response> performRequest(final Request request) throws IOException {
    final Timer.Context ctx = registry.timer("fauna-request").time();
    final SettableFuture<Response> rv = SettableFuture.create();

    client.prepareRequest(request)
      .addHeader("Authorization", authHeader)
      .execute(new AsyncCompletionHandler<Response>() {
        @Override
        public void onThrowable(Throwable t) {
          ctx.stop();
          rv.setException(t);
          logFailure(request, t);
        }

        @Override
        public Response onCompleted(Response response) throws Exception {
          ctx.stop();
          rv.set(response);
          logSuccess(request, response);
          return response;
        }
      });

    return rv;
  }

  private String mkUrl(String path) throws MalformedURLException {
    return new URL(faunaRoot, path).toString();
  }

  private void logSuccess(Request request, Response response) throws IOException {
    String requestData = Optional.fromNullable(request.getStringData()).or("");
    String faunaHost = Optional.fromNullable(response.getHeader(XFaunaDBHost)).or("Unknown");
    String faunaBuild = Optional.fromNullable(response.getHeader(XFaunaDBBuild)).or("Unknown");
    String responseBody = Optional.fromNullable(response.getResponseBody()).or("");

    log.debug("Request: " + request.getMethod() + " " + request.getUrl() + ": " + requestData + ". " +
      "Response: Status=" + response.getStatusCode() + ", Fauna Host: " + faunaHost + ", " +
      "Fauna Build: " + faunaBuild + ": " + responseBody);
  }

  private void logFailure(Request request, Throwable ex) {
    String requestData = Optional.fromNullable(request.getStringData()).or("");
    log.info("Request: " + request.getMethod() + " " + request.getUrl() + ": " + requestData + ". " +
      "Failed: " + ex.getMessage(), ex);
  }

  private static String generateAuthHeader(String authToken) throws UnsupportedEncodingException {
    return "Basic " + Base64.encode((authToken + ":").getBytes("ASCII"));
  }
}
