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
import java.util.Map;

public class Connection {
  public static Builder builder() {
    return new Builder();
  }

  static class Builder {
    private URL faunaRoot;
    private String authToken;
    private AsyncHttpClient client;
    private MetricRegistry metricRegistry;

    public Builder withFaunaRoot(String root) throws MalformedURLException {
      this.faunaRoot = new URL(root);
      return this;
    }

    public Builder withFaunaRoot(URL root) {
      this.faunaRoot = root;
      return this;
    }

    public Builder withAuthToken(String token) {
      this.authToken = token;
      return this;
    }

    public Builder withHttpClient(AsyncHttpClient client) {
      this.client = client;
      return this;
    }

    public Builder withMetrics(MetricRegistry registry) {
      this.metricRegistry = registry;
      return this;
    }

    public Connection build() throws UnsupportedEncodingException, MalformedURLException {
      MetricRegistry r;
      if (metricRegistry == null)
        r = new MetricRegistry();
      else
        r = metricRegistry;

      AsyncHttpClient c;
      if (client == null)
        c = new AsyncHttpClient();
      else
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

  public ListenableFuture<Response> get(String path) throws IOException {
    Request request = new RequestBuilder("GET")
      .setUrl(mkUrl(path))
      .build();

    return performRequest(request);
  }

  public ListenableFuture<Response> get(String path, Map<String, Collection<String>> params) throws IOException {
    Request request = new RequestBuilder("GET")
      .setUrl(mkUrl(path))
      .setParameters(params)
      .build();

    return performRequest(request);
  }

  public ListenableFuture<Response> post(String path, JsonNode body) throws IOException {
    Request request = new RequestBuilder("POST")
      .setUrl(mkUrl(path))
      .setBody(json.writeValueAsString(body))
      .setHeader(HttpHeaders.Names.CONTENT_TYPE, "application/json")
      .build();

    return performRequest(request);
  }

  public ListenableFuture<Response> put(String path) throws IOException {
    Request request = new RequestBuilder("PUT")
      .setUrl(mkUrl(path))
      .build();

    return performRequest(request);
  }

  public ListenableFuture<Response> put(String path, JsonNode body) throws IOException {
    Request request = new RequestBuilder("PUT")
      .setUrl(mkUrl(path))
      .setBody(json.writeValueAsString(body))
      .setHeader(HttpHeaders.Names.CONTENT_TYPE, "application/json")
      .build();

    return performRequest(request);
  }

  public ListenableFuture<Response> patch(String path, JsonNode body) throws IOException {
    Request request = new RequestBuilder("PATCH")
      .setUrl(mkUrl(path))
      .setBody(json.writeValueAsString(body))
      .setHeader(HttpHeaders.Names.CONTENT_TYPE, "application/json")
      .build();

    return performRequest(request);
  }

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

    log.debug("Request: " + request.getMethod() + " " + request.getURI() + ": " + requestData + ". " +
      "Response: Status=" + response.getStatusCode() + ", Fauna Host: " + faunaHost + ", " +
      "Fauna Build: " + faunaBuild + ": " + responseBody);
  }

  private void logFailure(Request request, Throwable ex) {
    String requestData = Optional.fromNullable(request.getStringData()).or("");
    log.info("Request: " + request.getMethod() + " " + request.getURI() + ": " + requestData + ". " +
      "Failed: " + ex.getMessage(), ex);
  }

  private static String generateAuthHeader(String authToken) throws UnsupportedEncodingException {
    return "Basic " + Base64.encode((authToken + ":").getBytes("ASCII"));
  }
}
