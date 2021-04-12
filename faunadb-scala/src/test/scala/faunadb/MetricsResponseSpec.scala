package faunadb

import faunadb.values.Metrics.Metrics
import faunadb.values.{LongV, Metrics, MetricsResponse}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class MetricsResponseSpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll {

  val metricsMap: Map[Metrics, String] = Map(
    Metrics.QueryBytesIn -> "35",
    Metrics.QueryBytesOut -> "75"
  )

  it should "return correct value object" in {
    val value = LongV(25L)
    val metricsResponse = MetricsResponse(value, metricsMap)

    metricsResponse.value shouldBe LongV(25L)
  }

  it should "return empty if metric not exists" in {
    val metricsResponse = MetricsResponse(null, metricsMap)

    metricsResponse.getMetric(Metrics.ByteReadOps) shouldBe None
  }

  it should "return correct metric value" in {
    val metricsResponse = MetricsResponse(null, metricsMap)

    metricsResponse.getMetric(Metrics.QueryBytesIn) shouldBe Some("35")
    metricsResponse.getMetric(Metrics.QueryBytesOut) shouldBe Some("75")
  }
}
