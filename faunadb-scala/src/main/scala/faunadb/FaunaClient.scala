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
import scala.compat.java8.FutureConverters._
import scala.concurrent.{ ExecutionContext, Future }

/** Companion object to the FaunaClient class. */
object FaunaClient {

  /**
    * Creates a new FaunaDB client.
    *
    *
    *
    * @param secret The secret material of the auth key used. See [[https://fauna.com/documentation#authentication-key_access]]
    * @param endpoint URL of the FaunaDB service to connect to. Defaults to https://db.fauna.com
    * @param metrics An optional [[com.codahale.metrics.MetricRegistry]] to record stats.
    * @return A configured FaunaClient instance.
    */
  def apply(
    secret: String = null,
    endpoint: String = null,
    metrics: MetricRegistry = null): FaunaClient = {

    val b = Connection.builder
    if (endpoint ne null) b.withFaunaRoot(endpoint)
    if (secret ne null) b.withAuthToken(secret)
    if (metrics ne null) b.withMetrics(metrics)
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
    * @return A [[scala.concurrent.Future]] containing the query result.
    *         The result is an instance of [[faunadb.values.Result]],
    *         which can be cast to a typed value using the
    *         [[faunadb.values.Field]] API. If the query fails, failed
    *         future is returned.
    */
  def query(expr: Expr)(implicit ec: ExecutionContext): Future[Value] = {
    def handleSuccessResponse(response: FullHttpResponse): Future[Value] = Future {
      val rv = json.treeToValue[Value](parseResponseBody(response).get("resource"), classOf[Value])
      if (rv eq null) NullV else rv
    }.andThen {
      case _ => response.release()
    }

    val response = connection.post("", json.valueToTree(expr)).toScala

    response
      .flatMap {
        case successResponse if successResponse.status().code() < 300 => handleSuccessResponse(successResponse)
        case errorResponse => handleErrorResponse(errorResponse)
      }
      .recoverWith(handleNetworkExceptions)
  }

  /**
    * Issues multiple queries as a single transaction.
    *
    * @param exprs the queries to run.
    * @return A [[scala.concurrent.Future]] containing an IndexedSeq of
    *         the results of each query. Each result is an instance of
    *         [[faunadb.values.Value]], which can be cast to a typed
    *         value using the [[faunadb.values.Field]] API. If *any*
    *         query fails, a failed future is returned.
    */
  def query(exprs: Iterable[Expr])(implicit ec: ExecutionContext): Future[IndexedSeq[Value]] = {
    def handleSuccessResponse(response: FullHttpResponse): Future[IndexedSeq[Value]] = Future {
      val arr = json.treeToValue[Value](parseResponseBody(response).get("resource"), classOf[Value])
      arr.asInstanceOf[ArrayV].elems
    }.andThen {
      case _ => response.release()
    }

    val response = connection.post("", json.valueToTree(exprs)).toScala

    response
      .flatMap {
        case successResponse if successResponse.status().code() < 300 => handleSuccessResponse(successResponse)
        case errorResponse => handleErrorResponse(errorResponse)
      }
      .recoverWith(handleNetworkExceptions)
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

  private def handleNetworkExceptions[A]: PartialFunction[Throwable, Future[A]] = {
    case ex: ConnectException => Future.failed(new UnavailableException(ex.getMessage, ex))
    case ex: TimeoutException => Future.failed(new TimeoutException(ex.getMessage))
  }

  private def handleErrorResponse(response: FullHttpResponse)(implicit ec: ExecutionContext): Future[Nothing] = {
    def parseError(): Future[QueryErrorResponse] = Future {
      val statusCode = response.status().code()
      val errors = Option(parseResponseBody(response).get("errors").asInstanceOf[ArrayNode])
      val parsedErrors = errors.map(_.iterator().asScala.map(json.treeToValue(_, classOf[QueryError])).toIndexedSeq).getOrElse(IndexedSeq.empty)
      val error = QueryErrorResponse(statusCode, parsedErrors)
      error
    }.recoverWith {
      case e: FaunaException => Future.failed(e)
      case unavailable if response.status().code() == 503 => Future.failed(new UnavailableException("Service Unavailable: Unparseable response.", unavailable))
      case unknown => Future.failed(new UnknownException(s"Unparseable service $unknown response.", unknown))
    }.andThen {
      case _ => response.release()
    }

    def parseErrorAndFailWith(fun: QueryErrorResponse => FaunaException): Future[Nothing] = {
      parseError().flatMap { errors =>
        val exception = fun(errors)
        Future.failed(exception)
      }
    }

    response.status().code() match {
      case 400 => parseErrorAndFailWith(new BadRequestException(_))
      case 401 => parseErrorAndFailWith(new UnauthorizedException(_))
      case 403 => parseErrorAndFailWith(new PermissionDeniedException(_))
      case 404 => parseErrorAndFailWith(new NotFoundException(_))
      case 500 => parseErrorAndFailWith(new InternalException(_))
      case 503 => parseErrorAndFailWith(new UnavailableException(_))
      case _   => parseErrorAndFailWith(new UnknownException(_))
    }
  }

  private def parseResponseBody(response: FullHttpResponse): JsonNode = {
    val body = json.readTree(new ByteBufInputStream(response.content()))
    if (body eq null) {
      throw new IOException("Invalid JSON.")
    } else {
      body
    }
  }
}
