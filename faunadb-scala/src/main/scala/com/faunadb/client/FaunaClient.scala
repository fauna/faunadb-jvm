package com.faunadb.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.faunadb.client.query.Expression
import com.faunadb.client.response.{Error, ResponseNode}
import com.faunadb.client.util.FutureImplicits._
import com.faunadb.httpclient.Connection
import com.ning.http.client.{Response => HttpResponse}

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

object FaunaClient {
  def apply(connection: Connection) = new FaunaClient(connection, new ObjectMapper())
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
 *  val client = FaunaClient(Connection.builder().withAuthToken("someAuthToken").build))
 *  val response = client.query(Get(Ref("some/ref")))
 * }}}
 */
class FaunaClient private (connection: Connection, json: ObjectMapper) {
  json.registerModule(new DefaultScalaModule)

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
          case _ =>
        }
      case _ =>
    }
  }

  private def handleQueryErrors(response: HttpResponse) = {
    response.getStatusCode match {
      case x if x >= 300 =>
        val errors = parseResponseBody(response).get("errors").asInstanceOf[ArrayNode]
        val parsedErrors = errors.iterator().asScala.map { json.treeToValue(_, classOf[Error]) }.toIndexedSeq
        val error = QueryErrorResponse(x, parsedErrors)
        x match {
          case 400 => throw new BadQueryException(error)
          case 404 => throw new NotFoundQueryException(error)
          case _ => throw new UnknownQueryException(error)
        }
      case _ =>
    }
  }

  private def parseResponseBody(response: HttpResponse) = {
    val body = response.getResponseBody("UTF-8")
    json.readTree(body)
  }
}
