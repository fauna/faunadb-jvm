package faunadb.values

import faunadb.values.Metrics.Metrics

case class MetricsResponse(value: Value, metricsMap: Map[Metrics, String]) {

  def getMetric(metric: Metrics): Option[String] = {
    metricsMap.get(metric)
  }
}
