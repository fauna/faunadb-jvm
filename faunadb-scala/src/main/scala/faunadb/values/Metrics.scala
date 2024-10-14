package faunadb.values

object Metrics extends Enumeration {
  type Metrics = Value

  val ByteReadOps = Value("x-byte-read-ops")
  val ByteWriteOps = Value("x-byte-write-ops")
  val ComputeOps = Value("x-compute-ops")
  val FaunaDbBuild = Value("x-faunadb-build")
  val QueryBytesIn = Value("x-query-bytes-in")
  val QueryBytesOut = Value("x-query-bytes-out")
  val QueryTime = Value("x-query-time")
  val StorageBytesRead = Value("x-storage-bytes-read")
  val StorageBytesWrite = Value("x-storage-bytes-write")
  val TxnRetries = Value("x-txn-retries")
  val TxnTime = Value("x-txn-time")

  val All = values.toList
}
