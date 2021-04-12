package com.faunadb.client.types;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * An aggregation type which consists of {@link Value} instance and extra information of response.
 * Extra information is stored in the metricsMap
 * and can be retrieved by using {@link #getMetric(Metrics)} method
 */
public class MetricsResponse {
    private final Map<Metrics, String> metricsMap;
    private final Value value;

    private MetricsResponse(Value value, Map<Metrics, String> metricsMap) {
        this.value = value;
        this.metricsMap = metricsMap;
    }

    public static MetricsResponse of(Value value, Map<Metrics, String> metricsMap) {
        return new MetricsResponse(value, metricsMap);
    }

    /**
     * Gets the {@link Value} part
     * @return the root node of the response tree
     */
    public Value getValue() {
        return value;
    }

    /**
     * Gets a specified metric
     * @param metric a metric user wants to retrieve
     * @return the metric value
     */
    public Optional<String> getMetric(Metrics metric) {
        return Optional.ofNullable(metricsMap.get(metric));
    }

    /**
     * Different header names that indicate the resources used in any query to faunadb server
     */
    public enum Metrics {
        BYTE_READ_OPS("x-byte-read-ops"),
        BYTE_WRITE_OPS("x-byte-write-ops"),
        COMPUTE_OPS("x-compute-ops"),
        FAUNADB_BUILD("x-faunadb-build"),
        QUERY_BYTES_IN("x-query-bytes-in"),
        QUERY_BYTES_OUT("x-query-bytes-out"),
        QUERY_TIME("x-query-time"),
        READ_OPS("x-read-ops"),
        STORAGE_BYTES_READ("x-storage-bytes-read"),
        STORAGE_BYTES_WRITE("x-storage-bytes-write"),
        TXN_RETRIES("x-txn-retries"),
        TXN_TIME("x-txn-time"),
        WRITE_OPS("x-write-ops");

        private final String metric;

        Metrics(String metric) {
            this.metric = metric;
        }

        public String getMetric() {
            return metric;
        }

        public static List<Metrics> vals() { return Arrays.asList(Metrics.values()); }
    }
}
