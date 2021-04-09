package faunadb.values

import faunadb.values.Metrics.Metrics

case class MetricsResponse(metricsMap: Map[Metrics, String], value: Value) {

  def getMetric(metric: Metrics): Option[String] = {
    metricsMap.get(metric)
  }
}
