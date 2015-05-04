package com.faunadb.httpclient

import com.fasterxml.jackson.databind.`type`.TypeFactory
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.{ObjectNode, ArrayNode}
import com.faunadb.query._

import com.ning.http.client.{Response => HttpResponse}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.collection.JavaConversions._

class FaunaClient(connection: Connection) {
  private val json = connection.json
  private val queryJson = json.copy()
  queryJson.registerModule(new SimpleModule().setDeserializerModifier(new FaunaDeserializerModifier))

  def query[R <: Response](expr: Expression)(implicit t: reflect.ClassTag[R]): Future[R] = {
    val body = queryJson.createObjectNode()
    body.set("q", queryJson.valueToTree(expr))
    connection.post("/", body).map { resp =>
      handleSimpleErrors(resp)
      handleQueryErrors(resp)
      val respBody = parseResponseBody(resp)
      val resource = respBody.get("resource").asInstanceOf[ObjectNode]
      queryJson.treeToValue(resource, t.runtimeClass).asInstanceOf[R]
    }
  }

  def querySet[R <: Response](expr: Expression)(implicit t: reflect.ClassTag[R]): Future[SetResponse[R]] = {
    val body = queryJson.createObjectNode()
    body.set("q", queryJson.valueToTree(expr))
    connection.post("/", body).map { resp =>
      handleSimpleErrors(resp)
      handleQueryErrors(resp)
      val jacksonType = TypeFactory.defaultInstance().constructParametrizedType(classOf[SetResponse[R]], classOf[SetResponse[R]], t.runtimeClass)
      val respBody = parseResponseBody(resp)
      val resource = respBody.get("resource").asInstanceOf[ObjectNode]
      queryJson.convertValue(resource, jacksonType).asInstanceOf[SetResponse[R]]
    }
  }

  // Java Compatibility
  def query[R <: Response](expr: Expression, clazz: java.lang.Class[R]): Future[R] = {
    val body = queryJson.createObjectNode()
    body.set("q", queryJson.valueToTree(expr))
    connection.post("/", body).map { resp =>
      handleSimpleErrors(resp)
      handleQueryErrors(resp)
      val respBody = parseResponseBody(resp)
      val resource = respBody.get("resource").asInstanceOf[ObjectNode]
      queryJson.treeToValue(resource, clazz)
    }
  }

  def querySet[R <: Response](expr: Expression, clazz: java.lang.Class[R]): Future[SetResponse[R]] = {
    val body = queryJson.createObjectNode()
    body.set("q", queryJson.valueToTree(expr))
    connection.post("/", body).map { resp =>
      handleSimpleErrors(resp)
      handleQueryErrors(resp)
      val jacksonType = TypeFactory.defaultInstance().constructParametrizedType(classOf[SetResponse[R]], classOf[SetResponse[R]], clazz)
      val respBody = parseResponseBody(resp)
      val resource = respBody.get("resource").asInstanceOf[ObjectNode]
      queryJson.convertValue(resource, jacksonType).asInstanceOf[SetResponse[R]]
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
        val parsedErrors = errors.iterator().map { queryJson.treeToValue(_, classOf[Error]) }.toSeq
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
