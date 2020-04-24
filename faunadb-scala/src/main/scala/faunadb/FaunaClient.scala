package faunadb

import com.codahale.metrics.MetricRegistry
import com.fasterxml.jackson.databind.{ JsonNode, ObjectMapper }
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.faunadb.common.Connection
import com.faunadb.common.Connection.JvmDriver
import faunadb.errors._
import faunadb.query.Expr
import faunadb.values.{ ArrayV, Encoder, NullV, Value }
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

  object QueryAPI {

    sealed trait QueryMagnet {
      type Result
      type PerformRequest = (JsonNode, Option[FiniteDuration], ExecutionContext) => Future[Value]

      def apply(json: ObjectMapper, performRequest: PerformRequest): Result
    }

    /**
      * ''Magnet companion'' for the [[faunadb.FaunaClient#query FaunaClient.query]] method.
      *
      *  The ''magnet branches'' listed within it define all the different variants
      *  the [[faunadb.FaunaClient#query FaunaClient.query]] method is overloaded.
      *
      *  @see [[http://spray.io/blog/2012-12-13-the-magnet-pattern/ Magnet Pattern]]
      */
    object QueryMagnet {

      /**
        * Implicitly converts from an [[faunadb.query.Expr Expr]] into
        * a valid [[faunadb.FaunaClient.QueryAPI.QueryMagnet QueryMagnet]].
        *
        * The [[faunadb.FaunaClient#query FaunaClient.query]] method can be called with the signature of this method.
        *
        * @param expr    the query to run.
        * @param ec      the `ExecutionContext` used to run the query asynchronously.
        * @param timeout the timeout for the given query. It replaces the timeout value set for the current
        *                [[faunadb.FaunaClient]] (if any) for the scope of this query. The timeout value has
        *                milliseconds precision.
        * @return        a [[faunadb.FaunaClient.QueryAPI.QueryMagnet QueryMagnet]]
        *                with return type [[scala.concurrent.Future Future]] of [[faunadb.values.Value Value]].
        */
      implicit def fromExpr(expr: Expr)(implicit ec: ExecutionContext, timeout: FiniteDuration = null) =
        new QueryMagnet {
          type Result = Future[Value]

          def apply(json: ObjectMapper, performRequest: PerformRequest): Result = {
            performRequest(json.valueToTree(expr), Option(timeout), ec).map { result =>
              if (result eq null) NullV else result
            }
          }
        }

      /**
        * Implicitly converts from an [[scala.collection.Iterable Iterable]] implementation
        * of [[faunadb.query.Expr Expr]] into a valid [[faunadb.FaunaClient.QueryAPI.QueryMagnet QueryMagnet]].
        *
        * The [[faunadb.FaunaClient#query FaunaClient.query]] method can be called with the signature of this method.
        *
        * @param exprs   the queries to run.
        * @param ec      the `ExecutionContext` used to run the query asynchronously.
        * @param timeout the timeout for the given query. It replaces the timeout value set for the current
        *                [faunadb.FaunaClient]] (if any) for the scope of this query. The timeout value has
        *                milliseconds precision.
        * @returna       a [[faunadb.FaunaClient.QueryAPI.QueryMagnet QueryMagnet]]
        *                with return type [[scala.concurrent.Future Future]]
        *                of [[scala.collection.Iterable Iterable]]
        *                of [[faunadb.values.Value Value]].
        */
      implicit def fromExprs(exprs: Iterable[Expr])(implicit ec: ExecutionContext, timeout: FiniteDuration = null) =
        new QueryMagnet {
          type Result = Future[IndexedSeq[Value]]

          def apply(json: ObjectMapper, performRequest: PerformRequest): Result = {
            performRequest(json.valueToTree(exprs), Option(timeout), ec).map { result =>
              result.asInstanceOf[ArrayV].elems
            }
          }
        }

      /**
        * Implicitly converts from an instance of type `A` into a valid [[faunadb.FaunaClient.QueryAPI.QueryMagnet QueryMagnet]].
        *
        * An implicit [[faunadb.values.Encoder Encoder]] of type `A` is required for converting the given value
        * into a valid [[faunadb.query.Expr Expr]].
        *
        * The [[faunadb.FaunaClient#query FaunaClient.query]] method can be called with the signature of this method.
        *
        * @param value   the query to run
        * @param ec      the `ExecutionContext` used to run the query asynchronously.
        * @param timeout the timeout for the given query. It replaces the timeout value set for the current
        *                [faunadb.FaunaClient]] (if any) for the scope of this query. The timeout value has
        *                milliseconds precision.
        * @return        a [[faunadb.FaunaClient.QueryAPI.QueryMagnet QueryMagnet]]
        *                with return type [[scala.concurrent.Future Future]] of [[faunadb.values.Value Value]].
        */
      implicit def fromAny[A: Encoder](value: A)(implicit ec: ExecutionContext, timeout: FiniteDuration = null) =
        new QueryMagnet {
          type Result = Future[Value]

          def apply(json: ObjectMapper, performRequest: PerformRequest): Result = {
            val expr = Expr.encode(value)
            performRequest(json.valueToTree(expr), Option(timeout), ec).map { result =>
              if (result eq null) NullV else result
            }
          }
        }
    }
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
  import FaunaClient._
  import QueryAPI._

  private[this] val json = new ObjectMapper
  json.registerModule(new DefaultScalaModule)

  /**
    * Issues a query.
    *
    * The query method is implemented following the [[http://spray.io/blog/2012-12-13-the-magnet-pattern/ Magnet Pattern]].
    * Refer to the ''magnet companion'' [[faunadb.FaunaClient.QueryAPI.QueryMagnet QueryMagnet]], for a definition of all
    * the possible ''magnet branches''.
    *
    * =Issuing queries=
    *
    * The query method requires in all its variants an implicit [[scala.concurrent.ExecutionContext ExecutionContext]].
    * The `ExecutionContext` is used for running the query against the FaunaDB server asynchronously.
    *
    * Make sure there is always an ''implicit'' `ExecutionContext` available in the scope, when calling this method.
    *
    * Example:
    *
    * {{{
    * implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global
    * }}}
    *
    * ==Issue a single query==
    *
    * Pass an instance of [[faunadb.query.Expr Expr]] as parameter:
    *
    * Example:
    *
    * {{{
    * import faunadb.values._
    * import faunadb.query._
    * import scala.concurrent.ExecutionContext.Implicits.global
    *
    * val result: Future[Value] =
    *   client.query(
    *     Get(Ref(Collection("users"), "263608813107544596"))
    *   )
    * }}}
    *
    * Alternatively, an instance of any class can be passed as parameter as long as there is
    * an implicit [[faunadb.values.Encoder Encoder]] for it in scope.
    *
    * Example:
    *
    * {{{
    * import faunadb.values._
    * import scala.concurrent.ExecutionContext.Implicits.global
    *
    * val result: Future[Value] =
    *   client.query(
    *     "Hello, FaunaDB!"
    *   )
    * }}}
    *
    * ==Issue multiple queries as a single transaction==
    *
    * Pass an [[scala.collection.Iterable Iterable]] implementation of [[faunadb.query.Expr Expr]] as parameter.
    *
    * Example:
    *
    * {{{
    * import faunadb.values._
    * import faunadb.query._
    * import scala.concurrent.ExecutionContext.Implicits.global
    *
    * val result: Future[IndexedSeq[Value]] =
    *   client.query(
    *     Seq(
    *       Get(Ref(Collection("users"), "263608813107544596")),
    *       Get(Ref(Collection("users"), "263608827286389268"))
    *     )
    *   )
    * }}}
    *
    * Alternatively, an [[scala.collection.Iterable Iterable]] implementation of any class can be passed as parameter,
    * as long as there is an implicit [[faunadb.values.Encoder Encoder]] for it in scope.
    *
    * Example:
    *
    * {{{
    * import faunadb.values._
    * import scala.concurrent.ExecutionContext.Implicits.global
    *
    * val result: Future[IndexedSeq[Value]] =
    *   client.query(
    *     Seq(
    *       "Hello",
    *      "FaunaDB!"
    *     )
    *   )
    * }}}
    *
    * =Other options=
    *
    * ==Set query timeout==
    *
    * The query method also accepts an ''implicit'' timeout parameter
    * of [[scala.concurrent.duration.FiniteDuration FiniteDuration]] type.
    *
    * Example:
    *
    * {{{
    * import faunadb.query._
    * import faunadb.values._
    *
    * import scala.concurrent.ExecutionContext.Implicits.global
    * import scala.concurrent.duration._
    *
    * implicit val timeout: FiniteDuration = 500 millis
    *
    * val result: Future[Value] =
    *   client.query(
    *     Get(Ref(Collection("users"), "263608813107544596"))
    *   )
    * }}}
    *
    * The timeout value defines the maximum time a query will be allowed
    * to run on the server. If the value is exceeded, the query is aborted.
    *
    * If a timeout value is provided when calling this method, it replaces
    * the timeout value set for this [[faunadb.FaunaClient]] (if any) for
    * the scope of this query.
    *
    * The timeout value has milliseconds precision.
    *
    * @see [[http://spray.io/blog/2012-12-13-the-magnet-pattern/ Magnet Pattern]]
    * @see [[faunadb.FaunaClient.QueryAPI.QueryMagnet QueryMagnet]]
    * */
  def query(magnet: QueryMagnet): magnet.Result = magnet(json, performRequest _)

  private def performRequest(body: JsonNode, timeout: Option[FiniteDuration], ec: ExecutionContext): Future[Value] = {
    implicit val executor = ec
    connection.post("", body, timeout.map(_.toJava).asJava).toScala.map { resp =>
      try {
        handleQueryErrors(resp)
        val rv = json.treeToValue[Value](parseResponseBody(resp).get("resource"), classOf[Value])
        if (rv eq null) NullV else rv
      } finally {
        resp.release()
      }
    }.recover(handleNetworkExceptions)
  }

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
