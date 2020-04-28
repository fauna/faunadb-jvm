package faunadb

import com.codahale.metrics.MetricRegistry
import com.fasterxml.jackson.databind.{ JsonNode, ObjectMapper }
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.faunadb.common.Connection
import com.faunadb.common.Connection.JvmDriver
import faunadb.errors._
import faunadb.query.Expr
import faunadb.values.{ ArrayV, NullV, Value }
import java.io.IOException
import java.net.ConnectException
import java.util.concurrent.TimeoutException

import io.netty.buffer.ByteBufInputStream
import io.netty.handler.codec.http.FullHttpResponse

import scala.collection.JavaConverters._
import scala.compat.java8.DurationConverters._
import scala.compat.java8.FutureConverters._
import scala.compat.java8.OptionConverters._
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.control.NonFatal

/** Companion object to the FaunaClient class. */
object FaunaClient {

  /**
    * Creates a new FaunaDB client.
    *
    * @param secret The secret material of the auth key used. See [[https://fauna.com/documentation#authentication-key_access]]
    * @param endpoint URL of the FaunaDB service to connect to. Defaults to https://db.fauna.com
    * @param metrics An optional [[com.codahale.metrics.MetricRegistry]] to record stats.
    * @param queryTimeout An optional global timeout for all the queries issued by this client. The timeout value has
    *                     milliseconds precision. If not provided, a default timeout value is set on the server side.
    * @return A configured FaunaClient instance.
    */
  def apply(
    secret: String = null,
    endpoint: String = null,
    metrics: MetricRegistry = null,
    queryTimeout: FiniteDuration = null): FaunaClient = {

    val b = Connection.builder
    if (endpoint ne null) b.withFaunaRoot(endpoint)
    if (secret ne null) b.withAuthToken(secret)
    if (metrics ne null) b.withMetrics(metrics)
    if (queryTimeout ne null) b.withQueryTimeout(queryTimeout.toJava)
    b.withJvmDriver(JvmDriver.SCALA)

    new FaunaClient(b.build)
  }
}

/**
  * The Scala native client for FaunaDB.
  *
  * Create a new client using [[faunadb.FaunaClient.apply]].
  *
  * Query requests are made asynchronously: All methods will return a
  * [[scala.concurrent.Future]].
  *
  * Example:
  * {{{
  * case class User(ref: RefV, name: String, age: Int)
  *
  * val client = FaunaClient(secret = "myKeySecret")
  *
  * val fut = client.query(Get(Ref(Class("users"), "123")))
  * val instance = Await.result(fut, 5.seconds)
  *
  * val userCast =
  *   for {
  *     ref <- instance("ref").to[RefV]
  *     name <- instance("data", "name").to[String]
  *     age <- instance("data", "age").to[Int]
  *   } yield {
  *     User(ref, name, age)
  *   }
  *
  * userCast.get
  * }}}
  *
  * @constructor create a new client with a configured [[com.faunadb.common.Connection]].
  */
class FaunaClient private (connection: Connection) {

  private[this] val json = new ObjectMapper
  json.registerModule(new DefaultScalaModule)

  /**
    * Issues a query.
    *
    * @param expr the query to run, created using the query dsl helpers in [[faunadb.query]].
    * @param ec the `ExecutionContext` used to run the query asynchronously.
    * @return A [[scala.concurrent.Future]] containing the query result.
    *         The result is an instance of [[faunadb.values.Result]],
    *         which can be cast to a typed value using the
    *         [[faunadb.values.Field]] API. If the query fails, failed
    *         future is returned.
    */
  def query(expr: Expr)(implicit ec: ExecutionContext): Future[Value] = query(expr, None)

  /**
    * Issues a query.
    *
    * @param expr the query to run, created using the query dsl helpers in [[faunadb.query]].
    * @param ec the `ExecutionContext` used to run the query asynchronously.
    * @param timeout the timeout for the current query. It replaces the timeout value set for this
    *                [[faunadb.FaunaClient]] if any for the scope of this query. The timeout value has
    *                milliseconds precision.
    * @return A [[scala.concurrent.Future]] containing the query result.
    *         The result is an instance of [[faunadb.values.Result]],
    *         which can be cast to a typed value using the
    *         [[faunadb.values.Field]] API. If the query fails, failed
    *         future is returned.
    */
  def query(expr: Expr, timeout: FiniteDuration)(implicit ec: ExecutionContext): Future[Value] = query(expr, Some(timeout))

  /**
    * Issues a query.
    *
    * @param expr the query to run, created using the query dsl helpers in [[faunadb.query]].
    * @param ec the `ExecutionContext` used to run the query asynchronously.
    * @param timeout the timeout for the current query. It replaces the timeout value set for this
    *                [[faunadb.FaunaClient]] if any for the scope of this query. The timeout value has
    *                milliseconds precision.
    * @return A [[scala.concurrent.Future]] containing the query result.
    *         The result is an instance of [[faunadb.values.Result]],
    *         which can be cast to a typed value using the
    *         [[faunadb.values.Field]] API. If the query fails, failed
    *         future is returned.
    */
  def query(expr: Expr, timeout: Option[FiniteDuration])(implicit ec: ExecutionContext): Future[Value] =
    performRequest(json.valueToTree(expr), timeout).map { result =>
      if (result eq null) NullV else result
    }

  /**
    * Issues multiple queries as a single transaction.
    *
    * @param exprs the queries to run.
    * @param ec the `ExecutionContext` used to run the query asynchronously.
    * @return A [[scala.concurrent.Future]] containing an IndexedSeq of
    *         the results of each query. Each result is an instance of
    *         [[faunadb.values.Value]], which can be cast to a typed
    *         value using the [[faunadb.values.Field]] API. If *any*
    *         query fails, a failed future is returned.
    */
  def query(exprs: Iterable[Expr])(implicit ec: ExecutionContext): Future[IndexedSeq[Value]] =
    query(exprs, None)

  /**
    * Issues multiple queries as a single transaction.
    *
    * @param exprs the queries to run.
    * @param ec the `ExecutionContext` used to run the query asynchronously.
    * @param timeout the timeout for the current query. It replaces the timeout value set for this
    *                [[faunadb.FaunaClient]] if any, for the scope of this query. The timeout value
    *                has milliseconds precision.
    * @return A [[scala.concurrent.Future]] containing an IndexedSeq of
    *         the results of each query. Each result is an instance of
    *         [[faunadb.values.Value]], which can be cast to a typed
    *         value using the [[faunadb.values.Field]] API. If *any*
    *         query fails, a failed future is returned.
    */
  def query(exprs: Iterable[Expr], timeout: FiniteDuration)(implicit ec: ExecutionContext): Future[IndexedSeq[Value]] =
    query(exprs, Some(timeout))

  /**
    * Issues multiple queries as a single transaction.
    *
    * @param exprs the queries to run.
    * @param ec the `ExecutionContext` used to run the query asynchronously.
    * @param timeout the timeout for the current query. It replaces the timeout value set for this
    *                [[faunadb.FaunaClient]] if any, for the scope of this query. The timeout value
    *                has milliseconds precision.
    * @return A [[scala.concurrent.Future]] containing an IndexedSeq of
    *         the results of each query. Each result is an instance of
    *         [[faunadb.values.Value]], which can be cast to a typed
    *         value using the [[faunadb.values.Field]] API. If *any*
    *         query fails, a failed future is returned.
    */
  def query(exprs: Iterable[Expr], timeout: Option[FiniteDuration])(implicit ec: ExecutionContext): Future[IndexedSeq[Value]] =
    performRequest(json.valueToTree(exprs), timeout).map { result =>
      result.asInstanceOf[ArrayV].elems
    }

  private def performRequest(body: JsonNode, timeout: Option[FiniteDuration])(implicit ec: ExecutionContext): Future[Value] =
    connection.post("", body, timeout.map(_.toJava).asJava).toScala.map { resp =>
      try {
        handleQueryErrors(resp)
        val result = json.treeToValue[Value](parseResponseBody(resp).get("resource"), classOf[Value])
        result
      } finally {
        resp.release()
      }
    }.recover(handleNetworkExceptions)

  /**
    * Creates a new scope to execute session queries. Queries submitted within the session scope will be
    * authenticated with the secret provided. A session client shares its parent's
    * [[com.faunadb.common.Connection]] instance and is closed as soon as the session scope ends.
    *
    * @param secret user secret for the session scope
    * @param session a function that receives a session client
    * @return the value produced by the session function
    */
  def sessionWith[A](secret: String)(session: FaunaClient => A): A = {
    val client = sessionClient(secret)
    try session(client) finally client.close()
  }

  /**
    * Create a new session client. The returned session client shares its parent [[com.faunadb.common.Connection]] instance.
    * The returned session client must be closed after its usage.
    *
    * @param secret user secret for the session client
    * @return a new session client
    */
  def sessionClient(secret: String): FaunaClient = new FaunaClient(connection.newSessionConnection(secret))

  /** Frees any resources held by the client and close the underlying connection. */
  def close(): Unit = connection.close()

  /**
   * Get the freshest timestamp reported to this client.
   */
  def lastTxnTime: Long = connection.getLastTxnTime

  /**
   * Sync the freshest timestamp seen by this client.
   *
   * This has no effect if staler than currently stored timestamp.
   * WARNING: This should be used only when coordinating timestamps across
   *          multiple clients. Moving the timestamp arbitrarily forward into
   *          the future will cause transactions to stall.
   */
  def syncLastTxnTime(timestamp: Long): Unit =
    connection.syncLastTxnTime(timestamp)

  private def handleNetworkExceptions[A]: PartialFunction[Throwable, A] = {
    case ex: ConnectException =>
      throw new UnavailableException(ex.getMessage, ex)
    case ex: TimeoutException =>
      throw new TimeoutException(ex.getMessage)
  }

  private def handleQueryErrors(response: FullHttpResponse) =
    response.status().code() match {
      case x if x >= 300 =>
        try {
          val errors = parseResponseBody(response).get("errors").asInstanceOf[ArrayNode]
          val parsedErrors = if (errors != null) {
            errors.iterator().asScala.map {
              json.treeToValue(_, classOf[QueryError])
            }
          } else {
            Seq.empty[QueryError]
          }

          val error = QueryErrorResponse(x, parsedErrors.toIndexedSeq)
          x match {
            case 400 => throw new BadRequestException(error)
            case 401 => throw new UnauthorizedException(error)
            case 403 => throw new PermissionDeniedException(error)
            case 404 => throw new NotFoundException(error)
            case 500 => throw new InternalException(error)
            case 503 => throw new UnavailableException(error)
            case _   => throw new UnknownException(error)
          }
        } catch {
          case e: FaunaException => throw e
          case NonFatal(ex)      => response.status().code() match {
            case 503   => throw new UnavailableException("Service Unavailable: Unparseable response.", ex)
            case s @ _ => throw new UnknownException(s"Unparseable service $s response.", ex)
          }
        }
      case _ =>
    }

  private def parseResponseBody(response: FullHttpResponse) = {
    val body = json.readTree(new ByteBufInputStream(response.content()))
    if (body eq null) {
      throw new IOException("Invalid JSON.")
    } else {
      body
    }
  }
}
