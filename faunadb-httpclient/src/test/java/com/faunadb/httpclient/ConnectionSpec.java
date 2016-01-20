package com.faunadb.httpclient;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableMap;
import com.ning.http.client.Response;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Test;

import java.io.IOException;
import java.util.SortedMap;
import java.util.concurrent.ExecutionException;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class ConnectionSpec {
  static ImmutableMap<String, String> config = getConfig();
  static final ObjectMapper json = new ObjectMapper();

  static String testDbName = "faunadb-httpclient-test-" + RandomStringUtils.randomAlphanumeric(8);

  // Cargo culted duplicated test code
  static ImmutableMap<String, String> getConfig() {
    String rootKey = System.getenv("FAUNA_ROOT_KEY");
    if (rootKey == null) throw new RuntimeException("FAUNA_ROOT_KEY must be defined to run tests");

    String domain = System.getenv("FAUNA_DOMAIN");
    if (domain == null) domain = "rest.faunadb.com";

    String scheme = System.getenv("FAUNA_SCHEME");
    if (scheme == null) scheme = "https";

    String port = System.getenv("FAUNA_PORT");
    if (port == null) port = "443";

    return ImmutableMap.<String, String>builder()
      .put("root_token", rootKey)
      .put("root_url", scheme + "://" + domain + ":" + port)
      .build();
  }

  static Connection mkConnection(MetricRegistry registry) throws IOException {
    return Connection.builder()
      .withFaunaRoot(config.get("root_url"))
      .withAuthToken(config.get("root_token"))
      .withMetrics(registry)
      .build();
  }

  @Test
  public void testStatsRecording() throws IOException, ExecutionException, InterruptedException {
    MetricRegistry registry = new MetricRegistry();
    Connection conn = mkConnection(registry);

    // this is easier than just manipulating the AST :(
    JsonNode q1 = json.readTree("{\n" +
      "    \"q\": {\n" +
      "        \"create\": {\n" +
      "            \"@ref\": \"databases\"\n" +
      "        },\n" +
      "        \"params\": {\n" +
      "            \"object\": {\n" +
      "                \"name\": \""+testDbName+"\"\n" +
      "            }\n" +
      "        }\n" +
      "    }\n" +
      "}");

    Response resp = conn.post("/", q1).get();

    SortedMap<String, Histogram> histograms = registry.getHistograms();
    assertThat(histograms.get("fauna-request-reported-io-columns-read"), notNullValue());
    assertThat(histograms.get("fauna-request-reported-io-columns-written"), notNullValue());
    assertThat(histograms.get("fauna-request-reported-io-counter-ops"), notNullValue());
    assertThat(histograms.get("fauna-request-reported-io-countercolumns-written"), notNullValue());
    assertThat(histograms.get("fauna-request-reported-io-insert-ops"), notNullValue());
    assertThat(histograms.get("fauna-request-reported-io-read-ops"), notNullValue());
    assertThat(histograms.get("fauna-request-reported-io-remove-ops"), notNullValue());
    assertThat(histograms.get("fauna-request-reported-io-stack-size"), notNullValue());
    assertThat(histograms.get("fauna-request-reported-io-transaction-count"), notNullValue());
    assertThat(histograms.get("fauna-request-reported-locktable-attempts"), notNullValue());
    assertThat(histograms.get("fauna-request-reported-locktable-successes"), notNullValue());
    assertThat(histograms.get("fauna-request-reported-locktable-unlocks"), notNullValue());

    SortedMap<String, Timer> timers = registry.getTimers();
    assertThat(timers.get("fauna-request-reported-http-request-processing-time"), notNullValue());
    assertThat(timers.get("fauna-request-reported-io-read-time"), notNullValue());
    assertThat(timers.get("fauna-request-reported-io-write-time"), notNullValue());
    assertThat(timers.get("fauna-request-reported-index-cached-getbysource"), notNullValue());
  }
}
