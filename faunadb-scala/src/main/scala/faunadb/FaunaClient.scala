package faunadb

import com.codahale.metrics.MetricRegistry
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.faunadb.httpclient.Connection
import com.ning.http.client.{ AsyncHttpClient, Response => HttpResponse }
import faunadb.errors._
import faunadb.query.Expr
import faunadb.util.FutureImplicits._
import faunadb.values.{ ArrayV, NullV, Value }
import java.io.IOException
import java.net.ConnectException
import java.util.concurrent.TimeoutException
import scala.collection.JavaConverters._
import scala.concurrent.{ ExecutionContext, Future }

/** Companion object to the FaunaClient class. */
object FaunaClient {

  /**
    * Creates a new FaunaDB client.
    *
    *
    *
    * @param secret The secret material of the auth key used. See [[https://faunadb.com/documentation#authentication-key_access]]
    * @param endpoint URL of the FaunaDB service to connect to. Defaults to https://rest.faunadb.com
    * @param metrics An optional [[com.codehale.metrics.MetricsRegistry]] to record stats.
    * @param httpClient An optional custom [[com.ning.http.client.AsyncHttpClient]].
    * @return A configured FaunaClient instance.
    */
  def apply(
    secret: String = null,
    endpoint: String = null,
    metrics: MetricRegistry = null,
    httpClient: AsyncHttpClient = null): FaunaClient = {

    val b = Connection.builder
    if (endpoint ne null) b.withFaunaRoot(endpoint)
    if (secret ne null) b.withAuthToken(secret)
    if (metrics ne null) b.withMetrics(metrics)
    if (httpClient ne null) b.withHttpClient(httpClient)

    new FaunaClient(b.build)
  }
}

/**
  * The Scala native client for FaunaDB.
  *
  * Create a new client using [[FaunaClient.apply]].
  *
  * Query requests are made asynchronously: All methods will return a
  * [[scala.concurrent.Future]].
  *
  * Example:
  * {{{
  * case class User(ref: Ref, name: String, age: Int)
  *
  * val client = FaunaClient(secret = "myKeySecret")
  *
  * val fut = client.query(Get(Ref("classes/users/123")))
  * val instance = Await.result(fut, 5.seconds)
  *
  * val userCast =
  *   for {
  *     ref <- instance("ref").as[Ref]
  *     name <- instance("data", "name").as[String]
  *     age <- instance("data", "age").as[Int]
  *   } yield {
  *     User(ref, name, age)
  *   }
  *
  * userCast.get
  * }}}
  *
  * @constructor create a new client with a configured [[com.faunadb.httpclient.Connection]].
  */
class FaunaClient(connection: Connection) {

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
  def query(expr: Expr)(implicit ec: ExecutionContext): Future[Value] =
    connection.post("/", json.valueToTree(expr)).asScalaFuture.map { resp =>
      handleQueryErrors(resp)
      val rv = json.treeToValue[Value](parseResponseBody(resp).get("resource"), classOf[Value])
      if (rv eq null) NullV else rv
    }.recover(handleNetworkExceptions)

  /**
    * Issues multiple queries as a single transaction.
    *
    * @param exprs the queries to run.
    * @return A [[scala.concurrent.Future]] containing an IndexedSeq of
    *         the results of each query. Each result is an instance of
    *         [faunadb.values.Result]], which can be cast to a typed
    *         value using the [[faunadb.values.Field]] API. If *any*
    *         query fails, a failed future is returned.
    */
  def query(exprs: Iterable[Expr])(implicit ec: ExecutionContext): Future[IndexedSeq[Value]] =
    connection.post("/", json.valueToTree(exprs)).asScalaFuture.map { resp =>
      handleQueryErrors(resp)
      val arr = json.treeToValue[Value](parseResponseBody(resp).get("resource"), classOf[Value])
      arr.asInstanceOf[ArrayV].elems
    }.recover(handleNetworkExceptions)

  /** Frees any resources held by the client and close the underlying connection. */
  def close(): Unit = connection.close()

  private def handleNetworkExceptions[A]: PartialFunction[Throwable, A] = {
    case ex: ConnectException =>
      throw new UnavailableException(ex.getMessage)
    case ex: TimeoutException =>
      throw new TimeoutException(ex.getMessage)
  }

  private def handleQueryErrors(response: HttpResponse) =
    response.getStatusCode match {
      case x if x >= 300 =>
        try {
          val errors = parseResponseBody(response).get("errors").asInstanceOf[ArrayNode]
          val parsedErrors = errors.iterator().asScala.map {
            json.treeToValue(_, classOf[QueryError])
          }.toIndexedSeq
          val error = QueryErrorResponse(x, parsedErrors)
          x match {
            case 400 => throw new BadRequestException(error)
            case 401 => throw new UnauthorizedException(error)
            case 404 => throw new NotFoundException(error)
            case 500 => throw new InternalException(error)
            case 503 => throw new UnavailableException(error)
            case _ => throw new UnknownException(error)
          }
        } catch {
          case ex: IOException =>
            response.getStatusCode match {
              case 503 => throw new UnavailableException("Service Unavailable: Unparseable response.")
              case s@_ => throw new UnknownException("Unparsable service " + s + "response.")
            }
        }
      case _ =>
    }

  private def parseResponseBody(response: HttpResponse) = {
    val body = response.getResponseBody("UTF-8")
    json.readTree(body)
  }
}
