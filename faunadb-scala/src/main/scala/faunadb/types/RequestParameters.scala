package faunadb.types

import com.faunadb.common.models.tags.Tag

import java.time.Duration
import scala.collection.JavaConversions._
import scala.compat.java8.DurationConverters.FiniteDurationops
import scala.compat.java8.OptionConverters.RichOptionForJava8
import scala.concurrent.duration.FiniteDuration

/**
 *
 * @param timeout the timeout for the current query. It replaces the timeout value set for this
 *                [[faunadb.FaunaClient]] if any for the scope of this query. The timeout value has
 *                milliseconds precision.
 * @param traceId A unique identifier for this query. Adheres to the
 *                [W3C Trace Context](https://w3c.github.io/trace-context) spec.
 * @param tags    Key-value pair metadata to associate with this query.
 */
class RequestParameters(val timeout: Option[FiniteDuration] = None,
                        val traceId: Option[String] = None,
                        val tags: Set[Tag] = Set()) {
  def timeoutAsJavaDuration: Option[Duration] = timeout.map(_.toJava)
  def asJava: com.faunadb.common.models.request.RequestParameters =
    new com.faunadb.common.models.request.RequestParameters(timeoutAsJavaDuration.asJava,
                                                            traceId.asJava,
                                                            setAsJavaSet(tags))
}