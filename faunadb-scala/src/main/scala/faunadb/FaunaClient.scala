package faunadb

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.faunadb.httpclient.Connection
import com.ning.http.client.{Response => HttpResponse}
import faunadb.errors._
import faunadb.query.Expr
import faunadb.values.{Value, LazyValue}
import faunadb.util.FutureImplicits._
import java.io.IOException
import java.net.ConnectException
import java.util.concurrent.TimeoutException
import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

/**
 * Methods to construct and obtain an instance of the FaunaDB client.
 */
object FaunaClient {
  /**
   * Constructs a new FaunaDB client with the provided HTTP Connection.
   *
   * See [[com.faunadb.httpclient.Connection.Builder]] for information on creating a Connection.
   */
  def apply(connection: Connection) = new FaunaClient(connection, new ObjectMapper())

  /**
   * Constructs a new FaunaDB client with the provided HTTP Connection and JSON ObjectMapper. This
   * can be used when custom JSON serialization and deserialization parameters are required.
   */
  def apply(connection: Connection, json: ObjectMapper) = new FaunaClient(connection, json.copy())
}

/**
 * The Scala native client for FaunaDB.
 *
 * Obtain an instance of the client using the methods on the companion object.
 *
 * The client is asynchronous, so all methods will return a [[scala.concurrent.Future]].
 *
 * Example:
 * {{{
 *  import faunadb.query._
 *
 *  val client = FaunaClient(Connection.builder().withAuthToken("someAuthToken").build))
 *  val response = client.query(Get(Ref("some/ref")))
 * }}}
 */
class FaunaClient private (connection: Connection, json: ObjectMapper) {
  json.registerModule(new DefaultScalaModule)

  /**
   * Issues a query to FaunaDB.
   *
   * Queries are modeled through the FaunaDB query language, represented by the case
   * classes in the [[faunadb.query]] package.
   *
   * Responses are modeled as a general response tree. Each node is a [[faunadb.types.Value]],
   * and can be coerced into structured types through various methods on that class.
   */
  def query(expr: Expr)(implicit ec: ExecutionContext): Future[Value] = {
    connection.post("/", json.valueToTree(expr)).asScalaFuture.map { resp =>
      handleQueryErrors(resp)
      val respBody = parseResponseBody(resp)
      val resource = respBody.get("resource")
      json.treeToValue(resource, classOf[LazyValue])
    }.recover(handleNetworkExceptions)
  }

  /**
   * Issues multiple queries to FaunaDB.
   *
   * These queries are sent to FaunaDB in a single request, where they are evaluated.
   * The list of responses is returned in the same order as the issued queries.
   *
   */
  def query(exprs: Iterable[Expr])(implicit ec: ExecutionContext): Future[IndexedSeq[LazyValue]] = {
    connection.post("/", json.valueToTree(exprs)).asScalaFuture.map { resp =>
      handleQueryErrors(resp)
      val respBody = parseResponseBody(resp)
      respBody.get("resource").asInstanceOf[ArrayNode].asScala.map { node =>
        json.treeToValue(node, classOf[LazyValue])
      }.toIndexedSeq
    }.recover(handleNetworkExceptions)
  }

  /**
   * Frees any resources held by the client. Also closes the underlying Connection.
   */
  def close(): Unit = {
    connection.close()
  }

  private def handleNetworkExceptions[A]: PartialFunction[Throwable, A] = {
    case ex: ConnectException =>
      throw new UnavailableException(ex.getMessage)
    case ex: TimeoutException =>
      throw new TimeoutException(ex.getMessage)
  }

  private def handleQueryErrors(response: HttpResponse) = {
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
  }

  private def parseResponseBody(response: HttpResponse) = {
    val body = response.getResponseBody("UTF-8")
    json.readTree(body)
  }
}
