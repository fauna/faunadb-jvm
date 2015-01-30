package com.faunadb.httpclient;

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.AsyncHttpClientConfig;

import java.net.MalformedURLException;
import java.net.URL;

public class ConnectionBuilder {
    private final String authToken;
    private final String faunaRoot;
    private final AsyncHttpClientConfig clientConfig;
    private final MetricRegistry metricRegistry;
    private final ObjectMapper objectMapper;

    public ConnectionBuilder(String authToken, String faunaRoot, AsyncHttpClientConfig clientConfig, MetricRegistry metricRegistry, ObjectMapper objectMapper) {
        this.authToken = authToken;
        this.faunaRoot = faunaRoot;
        this.clientConfig = clientConfig;
        this.metricRegistry = metricRegistry;
        this.objectMapper = objectMapper;
    }

    public static ConnectionBuilder create() {
        return new ConnectionBuilder(
            "", Connection.DefaultRoot(), Connection.DefaultConfig(), new MetricRegistry(), new ObjectMapper());
    }


    public ConnectionBuilder withAuthToken(String newAuthToken) {
        return new ConnectionBuilder(newAuthToken, faunaRoot, clientConfig, metricRegistry, objectMapper);
    }

    public ConnectionBuilder withFaunaRoot(String newRoot) {
        return new ConnectionBuilder(authToken, newRoot, clientConfig, metricRegistry, objectMapper);
    }

    public ConnectionBuilder withClientConfig(AsyncHttpClientConfig newClientConfig) {
        return new ConnectionBuilder(authToken, faunaRoot, newClientConfig, metricRegistry, objectMapper);
    }

    public ConnectionBuilder withMetricRegistry(MetricRegistry newMetricRegistry) {
        return new ConnectionBuilder(authToken, faunaRoot, clientConfig, newMetricRegistry, objectMapper);
    }

    public ConnectionBuilder withJson(ObjectMapper newObjectMapper) {
        return new ConnectionBuilder(authToken, faunaRoot, clientConfig, metricRegistry, newObjectMapper);
    }

    public Connection build() throws MalformedURLException {
        return new Connection(new URL(faunaRoot), authToken, new AsyncHttpClient(clientConfig), metricRegistry, objectMapper);
    }
}
