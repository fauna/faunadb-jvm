package faunadb.values

case class MetricsResponse(metricsMap: Map[String, Option[String]], value: Value) {

  def getMetric(metric: String): Option[String] = {
    metricsMap.getOrElse(metric, None)
  }
}

object Metrics {
  val BYTE_READ_OPS = "x-byte-read-ops"
  val BYTE_WRITE_OPS = "x-byte-write-ops"
  val COMPUTE_OPS = "x-compute-ops"
  val FAUNADB_BUILD = "x-faunadb-build"
  val QUERY_BYTES_IN = "x-query-bytes-in"
  val QUERY_BYTES_OUT = "x-query-bytes-out"
  val QUERY_TIME = "x-query-time"
  val READ_OPS = "x-read-ops"
  val STORAGE_BYTES_READ = "x-storage-bytes-read"
  val STORAGE_BYTES_WRITE = "x-storage-bytes-write"
  val TXN_RETRIES = "x-txn-retries"
  val TXN_TIME = "x-txn-time"
  val WRITE_OPS = "x-write-ops"

  val list = List(
    BYTE_READ_OPS, BYTE_WRITE_OPS, COMPUTE_OPS, FAUNADB_BUILD, QUERY_BYTES_IN,
    QUERY_BYTES_OUT, QUERY_TIME, READ_OPS, STORAGE_BYTES_READ, STORAGE_BYTES_WRITE,
    TXN_RETRIES, TXN_TIME, WRITE_OPS
  )
}
