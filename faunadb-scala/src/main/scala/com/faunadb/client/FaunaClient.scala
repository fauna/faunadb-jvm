package com.faunadb.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.`type`.TypeFactory
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.{ObjectNode, ArrayNode}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.faunadb.client.query.{FaunaDeserializerModifier, Response, Expression}
import com.faunadb.httpclient.Connection
import com.ning.http.client.{Response => HttpResponse}

import com.faunadb.client.util.FutureImplicits._
import scala.concurrent.{ExecutionContext, Future}
import scala.collection.JavaConverters._

object FaunaClient {
  def apply(connection: Connection) = new FaunaClient(connection, new ObjectMapper())
  def apply(connection: Connection, json: ObjectMapper) = new FaunaClient(connection, json.copy())
}

class FaunaClient(connection: Connection, json: ObjectMapper) {
  json.registerModule(new DefaultScalaModule)
  json.registerModule(new SimpleModule().setDeserializerModifier(new FaunaDeserializerModifier))

  def query[R <: Response](expr: Expression)(implicit t: reflect.ClassTag[R], ec: ExecutionContext): Future[R] = {
    val body = json.createObjectNode()
    body.set("q", json.valueToTree(expr))
    connection.post("/", body).asScalaFuture.map { resp =>
      handleSimpleErrors(resp)
      handleQueryErrors(resp)
      val respBody = parseResponseBody(resp)
      val resource = respBody.get("resource").asInstanceOf[ObjectNode]
      json.treeToValue(resource, t.runtimeClass).asInstanceOf[R]
    }
  }

  def querySet[R <: Response](expr: Expression)(implicit t: reflect.ClassTag[R], ec: ExecutionContext): Future[SetResponse[R]] = {
    val body = json.createObjectNode()
    body.set("q", json.valueToTree(expr))
    connection.post("/", body).asScalaFuture.map { resp =>
      handleSimpleErrors(resp)
      handleQueryErrors(resp)
      val jacksonType = TypeFactory.defaultInstance().constructParametrizedType(classOf[SetResponse[R]], classOf[SetResponse[R]], t.runtimeClass)
      val respBody = parseResponseBody(resp)
      val resource = respBody.get("resource").asInstanceOf[ObjectNode]
      json.convertValue(resource, jacksonType).asInstanceOf[SetResponse[R]]
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
        val parsedErrors = errors.iterator().asScala.map { json.treeToValue(_, classOf[com.faunadb.client.query.Error]) }.toSeq
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
