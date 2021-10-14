package faunadb

import com.codahale.metrics.MetricRegistry
import com.fasterxml.jackson.databind.{DeserializationFeature, JsonNode, ObjectMapper}
import com.fasterxml.jackson.databind.node.{ArrayNode, NullNode}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.faunadb.common.Connection
import com.faunadb.common.Connection.JvmDriver
import faunadb.errors._
import faunadb.query.{Expr, Get}
import faunadb.values.{ArrayV, Metrics, MetricsResponse, NullV, Value}

import java.io.IOException
import java.net.ConnectException
import java.net.http.HttpResponse
import java.util.concurrent.{CompletionException, Flow, TimeoutException}
import com.faunadb.common.http.ResponseBodyStringProcessor
import faunadb.FaunaClient.EventField
import faunadb.streaming.{BodyValueFlowProcessor, SnapshotEventFlowProcessor}

import scala.collection.JavaConverters._
import scala.compat.java8.DurationConverters._
import scala.compat.java8.FutureConverters._
import scala.compat.java8.OptionConverters._
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

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
    * @param userAgent A value used for the User-Agent HTTP header.
    * @return A configured FaunaClient instance.
    */
  def apply(
    secret: String = null,
    endpoint: String = null,
    metrics: MetricRegistry = null,
    queryTimeout: FiniteDuration = null,
    userAgent: String = null,
    checkNewVersion: Boolean = true,
    customHeaders: Map[String, String] = Map.empty): FaunaClient = {

    val b = Connection.builder
    if (endpoint ne null) b.withFaunaRoot(endpoint)
    if (secret ne null) b.withAuthToken(secret)
    if (metrics ne null) b.withMetrics(metrics)
    if (queryTimeout ne null) b.withQueryTimeout(queryTimeout.toJava)
    if (customHeaders.nonEmpty) b.withCustomHeaders(customHeaders.asJava)
    b.withJvmDriver(JvmDriver.SCALA)
    b.withScalaVersion(util.Properties.versionNumberString)
    b.withUserAgent(userAgent)
    b.withCheckNewDriverVersion(checkNewVersion)

    new FaunaClient(b.build)
  }

  sealed abstract class EventField(val value: String)
  case object DocumentField extends EventField("document")
  case object PrevField extends EventField("prev")
  case object DiffField extends EventField("diff")
  case object ActionField extends EventField("action")
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
  json.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
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
    performRequest(json.valueToTree(expr), timeout)

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
  def queryWithMetrics(expr: Expr, timeout: Option[FiniteDuration])(implicit ec: ExecutionContext): Future[MetricsResponse] =
    performRequestWithMetrics(json.valueToTree(expr), timeout)

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

  private def performRequestCommon[T](body: JsonNode, timeout: Option[FiniteDuration], handler: HttpResponse[String] => Future[T])(implicit ec: ExecutionContext): Future[T] = {
    val javaTimeout = timeout.map(_.toJava).asJava
    val response: Future[HttpResponse[String]] = connection.post("", body, javaTimeout).toScala

    response
      .flatMap {
        case successResponse if successResponse.statusCode() < 300 => handler(successResponse)
        case errorResponse => handleErrorResponse(errorResponse.statusCode(), errorResponse.body())
      }
      .recoverWith(handleNetworkExceptions)
  }

  private def performRequest(body: JsonNode, timeout: Option[FiniteDuration])(implicit ec: ExecutionContext): Future[Value] = {
    performRequestCommon(body, timeout, handleSuccessResponse)
  }

  private def performRequestWithMetrics(body: JsonNode, timeout: Option[FiniteDuration])(implicit ec: ExecutionContext): Future[MetricsResponse] = {
    performRequestCommon(body, timeout, handleSuccessResponseWithMetrics)
  }

  private def handleSuccessResponseWithMetrics(response: HttpResponse[String])(implicit ec: ExecutionContext): Future[MetricsResponse] = {
    val metricsMap = Metrics.All
      .iterator
      .map(item => item -> response.headers().firstValue(item.toString).asScala)
      .collect {
        case (key, Some(value)) => key -> value
      }
      .toMap

    handleSuccessResponse(response).map(item => MetricsResponse(item, metricsMap))
  }

  /**
    * Creates a subscription to the result of the given read-only expression. When
    * executed, the expression must only perform reads and produce a single
    * streamable type, such as a reference or a version. Expressions that attempt
    * to perform writes or produce non-streamable types will result in an error.
    * Otherwise, any expression can be used to initiate a stream, including
    * user-defined function calls.
    *
    * @param expr the query to subscribe to.
    * @param fields fields to opt-in on the events.
    * @param snapshot if true the second event will be a snapshot event of the target
    * @param ec the `ExecutionContext` used to run the query asynchronously.
    * @return A [[scala.concurrent.Future]] containing a [[java.util.concurrent.Flow.Publisher]] which yields element of
    *         type [[faunadb.values.Value]]. The [[scala.concurrent.Future]] fails if the stream cannot be setup.
    */
  def stream(expr: Expr, fields: Seq[EventField] = Nil, snapshot: Boolean = false)(implicit ec: ExecutionContext): Future[Flow.Publisher[Value]] =
    performStreamRequest(json.valueToTree(expr), fields).map { valuePublisher =>
      if (snapshot) {
        val documentValueFlowProcessor = new SnapshotEventFlowProcessor(() => query(Get(expr)))
        valuePublisher.subscribe(documentValueFlowProcessor)
        documentValueFlowProcessor
      } else {
        valuePublisher
      }
    }

  private def performStreamRequest(body: JsonNode, fields: Seq[EventField] = Nil)(implicit ec: ExecutionContext): Future[Flow.Publisher[Value]] = {
    val params = Map("fields" -> fields.iterator.map(_.value).toList.asJava).asJava
    connection.performStreamRequest("POST", "stream", body, params)
      .toScala
      .flatMap {
        case successResponse if successResponse.statusCode() < 300 =>
          // Subscribe a new FlowEventValueProcessor to consume the Body's Flow.Publisher
          val flowEventValueProcessor = new BodyValueFlowProcessor(json, txn => syncLastTxnTime(txn), () => connection.performStreamRequest("POST", "stream", body, params))
          successResponse.body().subscribe(flowEventValueProcessor)
          Future.successful(flowEventValueProcessor)
        case errorResponse =>
          // The request failed, we need to consume the body manually for error reporting
          ResponseBodyStringProcessor.consumeBody(errorResponse)
            .toScala
            .flatMap(errorBody => handleErrorResponse(errorResponse.statusCode(), errorBody))
      }
      .recoverWith(handleNetworkExceptions)
  }


  /**
    * Creates a new scope to execute session queries. Queries submitted within the session scope will be
    * authenticated with the secret provided. A session client shares its parent's [[com.faunadb.common.Connection]]
    * instance.
    *
    * @param secret user secret for the session scope
    * @param session a function that receives a session client
    * @return the value produced by the session function
    */
  def sessionWith[A](secret: String)(session: FaunaClient => A): A = {
    val client = sessionClient(secret)
    session(client)
  }

  /**
    * Create a new session client. The returned session client shares its parent [[com.faunadb.common.Connection]]
    * instance.
    *
    * @param secret user secret for the session client
    * @return a new session client
    */
  def sessionClient(secret: String): FaunaClient = new FaunaClient(connection.newSessionConnection(secret))

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

  private def parseResponseBody(responseBody: String)(implicit ec: ExecutionContext): Future[JsonNode] = {
    def parse: Future[Option[JsonNode]] = Future(Option(json.readTree(responseBody)))

    parse.flatMap {
      case Some(json) => Future.successful(json)
      case None => Future.failed(new IOException("Invalid JSON."))
    }
  }

  private def handleSuccessResponse(response: HttpResponse[String])(implicit ec: ExecutionContext): Future[Value] = {
    def getResource(body: JsonNode): Future[JsonNode] = Option(body.get("resource")) match {
      case Some(resource) => Future.successful(resource)
      case None => Future.failed(new IOException("Invalid JSON."))
    }

    def parseValue(resource: JsonNode): Future[Value] = resource match {
      case _: NullNode => Future.successful(NullV)
      case _: JsonNode => Future(json.treeToValue[Value](resource, classOf[Value]))
    }

    for {
      body <- parseResponseBody(response.body())
      resource <- getResource(body)
      value <- parseValue(resource)
    } yield value
  }

  private def handleErrorResponse(statusCode: Int, responseBody: String)(implicit ec: ExecutionContext): Future[Nothing] = {
    def parseErrors(): Future[QueryErrorResponse] = {
      def getErrors(body: JsonNode): Future[Iterator[JsonNode]] = Option(body.get("errors")) match {
        case Some(errors: ArrayNode) => Future.successful(errors.iterator().asScala)
        case _ => Future.successful(Iterator.empty)
      }

      def parseErrors(errors: Iterator[JsonNode]): Future[IndexedSeq[QueryError]] = Future {
        errors.map(json.treeToValue(_, classOf[QueryError])).toIndexedSeq
      }

      val result: Future[QueryErrorResponse] =
        for {
          body <- parseResponseBody(responseBody)
          errors <- getErrors(body)
          queryErrors <- parseErrors(errors)
        } yield QueryErrorResponse(statusCode, queryErrors)

      result
        .recoverWith {
          case e: FaunaException => Future.failed(e)
          case unavailable if statusCode == 503 => Future.failed(new UnavailableException("Service Unavailable: Unparseable response.", unavailable))
          case unknown => Future.failed(new UnknownException(s"Unparseable service $unknown response.", unknown))
        }
    }

    def parseErrorsAndFailWith(fun: QueryErrorResponse => FaunaException): Future[Nothing] = {
      parseErrors().flatMap { errors =>
        val exception = fun(errors)
        Future.failed(exception)
      }
    }

    statusCode match {
      case 400 => parseErrorsAndFailWith(new BadRequestException(_))
      case 401 => parseErrorsAndFailWith(new UnauthorizedException(_))
      case 403 => parseErrorsAndFailWith(new PermissionDeniedException(_))
      case 404 => parseErrorsAndFailWith(new NotFoundException(_))
      case 409 => parseErrorsAndFailWith(new TransactionContentionException(_))
      case 500 => parseErrorsAndFailWith(new InternalException(_))
      case 503 => parseErrorsAndFailWith(new UnavailableException(_))
      case _   => parseErrorsAndFailWith(new UnknownException(_))
    }
  }

  private def handleNetworkExceptions[A]: PartialFunction[Throwable, Future[A]] = {
    case ex: ConnectException =>
      Future.failed(new UnavailableException(ex.getMessage, ex))
    case ex: TimeoutException =>
      Future.failed(new UnavailableException(ex.getMessage, ex))
    case ex: CompletionException if ex.getCause.isInstanceOf[IOException] && ex.getMessage.contains("header parser received no bytes") =>
      Future.failed(new UnavailableException(ex.getMessage, ex))
    case ex: CompletionException if ex.getCause.isInstanceOf[IOException] && ex.getMessage.contains("too many concurrent streams") =>
      Future.failed(BadRequestException(None, "the maximum number of streams has been reached for this client"))
  }

}

