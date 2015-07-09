package faunadb

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import faunadb.errors._
import faunadb.query.Expression
import faunadb.response.ResponseNode
import faunadb.util.FutureImplicits._
import com.faunadb.httpclient.Connection
import com.ning.http.client.{Response => HttpResponse}

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
 *  import com.faunadb.client.query.Language._
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
   * classes in the [[com.faunadb.client.query]] package.
   *
   * Responses are modeled as a general response tree. Each node is a [[response.ResponseNode]],
   * and can be coerced into structured types through various methods on that class.
   */
  def query(expr: Expression)(implicit ec: ExecutionContext): Future[ResponseNode] = {
    val body = json.createObjectNode()
    body.set("q", json.valueToTree(expr))
    connection.post("/", body).asScalaFuture.map { resp =>
      handleSimpleErrors(resp)
      handleQueryErrors(resp)
      val respBody = parseResponseBody(resp)
      val resource = respBody.get("resource")
      json.treeToValue(resource, classOf[ResponseNode])
    }
  }

  /**
   * Issues multiple queries to FaunaDB.
   *
   * These queries are sent to FaunaDB in a single request, where they are evaluated.
   * The list of responses is returned in the same order as the issued queries.
   *
   */
  def query(exprs: Iterable[Expression])(implicit ec: ExecutionContext): Future[IndexedSeq[ResponseNode]] = {
    val body = json.createObjectNode()
    body.set("q", json.valueToTree(exprs))
    connection.post("/", body).asScalaFuture.map { resp =>
      handleSimpleErrors(resp)
      handleQueryErrors(resp)
      val respBody = parseResponseBody(resp)
      respBody.get("resource").asInstanceOf[ArrayNode].asScala.map { node =>
        json.treeToValue(node, classOf[ResponseNode])
      }.toIndexedSeq
    }
  }

  /**
   * Frees any resources held by the client. Also closes the underlying Connection.
   */
  def close(): Unit = {
    connection.close()
  }

  private def handleSimpleErrors(response: HttpResponse) = {
    response.getStatusCode match {
      case x if x >= 300 =>
        x match {
          case 401 =>
            val error = parseResponseBody(response).get("error").asText()
            throw new UnauthorizedException(error)
          case 500 =>
            val error = parseResponseBody(response).get("error").asText()
            throw new InternalException(error)
          case _ =>
        }
      case _ =>
    }
  }

  private def handleQueryErrors(response: HttpResponse) = {
    response.getStatusCode match {
      case x if x >= 300 =>
        val errors = parseResponseBody(response).get("errors").asInstanceOf[ArrayNode]
        val parsedErrors = errors.iterator().asScala.map { json.treeToValue(_, classOf[QueryError]) }.toIndexedSeq
        val error = QueryErrorResponse(x, parsedErrors)
        x match {
          case 400 => throw new BadRequestException(error)
          case 404 => throw new NotFoundException(error)
          case _ => throw new UnknownException(error)
        }
      case _ =>
    }
  }

  private def parseResponseBody(response: HttpResponse) = {
    val body = response.getResponseBody("UTF-8")
    json.readTree(body)
  }
}
