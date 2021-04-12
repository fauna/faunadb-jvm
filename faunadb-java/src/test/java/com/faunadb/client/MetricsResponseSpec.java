package com.faunadb.client;

import com.faunadb.client.types.MetricsResponse;
import com.faunadb.client.types.Value;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;

public class MetricsResponseSpec {

    Map<MetricsResponse.Metrics, String> metricsMap;

    @Before
    public void setUp() {
        metricsMap = Map.of(
                MetricsResponse.Metrics.QUERY_BYTES_IN, "35",
                MetricsResponse.Metrics.QUERY_BYTES_OUT, "75"
        );
    }

    @Test
    public void shouldReturnCorrectValueObject() {
        Value value = new Value.LongV(25L);
        MetricsResponse metricsResponse = MetricsResponse.of(value, metricsMap);

        assertEquals(new Value.LongV(25L), metricsResponse.getValue());
    }

    @Test
    public void shouldReturnEmptyIfMetricNotExists() {
        MetricsResponse metricsResponse = MetricsResponse.of(null, metricsMap);

        assertEquals(Optional.empty(), metricsResponse.getMetric(MetricsResponse.Metrics.BYTE_READ_OPS));
    }

    @Test
    public void shouldReturnCorrectMetricValue() {
        MetricsResponse metricsResponse = MetricsResponse.of(null, metricsMap);

        assertEquals(Optional.of("35"), metricsResponse.getMetric(MetricsResponse.Metrics.QUERY_BYTES_IN));
        assertEquals(Optional.of("75"), metricsResponse.getMetric(MetricsResponse.Metrics.QUERY_BYTES_OUT));
    }
}
