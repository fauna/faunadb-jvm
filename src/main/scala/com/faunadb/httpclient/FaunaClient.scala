package com.faunadb.httpclient

import com.fasterxml.jackson.databind.`type`.TypeFactory
import com.fasterxml.jackson.databind.module.SimpleModule
import com.faunadb.query.{Ref, Expression, Response, FaunaDeserializerModifier}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class SetResponse[R <: Response](data: Array[R], before: Option[Ref], after: Option[Ref])

class FaunaClient(connection: Connection) {
  private val json = connection.json
  private val queryJson = json.copy()
  queryJson.registerModule(new SimpleModule().setDeserializerModifier(new FaunaDeserializerModifier))

  def query[R <: Response](expr: Expression, clazz: Class[R]): Future[R] = query(expr)

  def query[R <: Response](expr: Expression)(implicit t: reflect.ClassTag[R]): Future[R] = {
    val body = queryJson.createObjectNode()
    body.set("q", queryJson.valueToTree(expr))
    connection.post("/", body).map { resp =>
      queryJson.treeToValue(resp.resource, t.runtimeClass).asInstanceOf[R]
    }
  }

  def querySet[R <: Response](expr: Expression, clazz: Class[R]): Future[SetResponse[R]] = {
    val body = queryJson.createObjectNode()
    body.set("q", queryJson.valueToTree(expr))
    connection.post("/", body).map { resp =>
      val jacksonType = TypeFactory.defaultInstance().constructParametricType(classOf[SetResponse[R]], clazz)
      queryJson.convertValue(resp.resource, jacksonType).asInstanceOf[SetResponse[R]]
    }
  }

  def querySet[R <: Response](expr: Expression)(implicit t: reflect.ClassTag[R]): Future[SetResponse[R]] = {
    val body = queryJson.createObjectNode()
    body.set("q", queryJson.valueToTree(expr))
    connection.post("/", body).map { resp =>
      val jacksonType = TypeFactory.defaultInstance().constructParametricType(classOf[SetResponse[R]], t.runtimeClass)
      queryJson.convertValue(resp.resource, jacksonType).asInstanceOf[SetResponse[R]]
    }
  }
}

