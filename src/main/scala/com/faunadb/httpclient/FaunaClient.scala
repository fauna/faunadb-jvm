package com.faunadb.httpclient

import com.fasterxml.jackson.databind.module.SimpleModule
import com.faunadb._
import com.faunadb.query.{Expression, Response, FaunaDeserializerModifier}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class FaunaClient(connection: Connection) {
  private val json = connection.json
  private val queryJson = json.copy()
  queryJson.registerModule(new SimpleModule().setDeserializerModifier(new FaunaDeserializerModifier))

  def query[R <: Response](expr: Expression)(implicit t: reflect.ClassTag[R]): Future[R] = {
    val body = queryJson.createObjectNode()
    body.set("q", queryJson.valueToTree(expr))
    connection.post("/", body).map { resp =>
      queryJson.treeToValue(resp.resource, t.runtimeClass).asInstanceOf[R]
    }
  }

  private def postOrPut(instance: FaunaInstance) = {
    val body = json.createObjectNode()
    body.set("data", instance.data)
    if (instance.ref.isEmpty) {
      val uri = "/" + instance.classRef
      connection.post(uri, body)
    } else {
      val uri = "/" + instance.ref
      connection.put(uri, body)
    }
  }

  def createDatabase(dbName: String): Future[FaunaDatabase] = {
    val instance = new FaunaInstance(ref="databases/"+dbName, classRef="databases")
    postOrPut(instance) map { resp =>
      json.treeToValue(resp.resource, classOf[FaunaDatabase])
    }
  }

  def createClass(classRef: String): Future[FaunaClass] = {
    val instance = new FaunaInstance(ref=classRef, classRef="classes")
    postOrPut(instance) map { resp =>
      json.treeToValue(resp.resource, classOf[FaunaClass])
    }
  }

  def createKey(database: String, role: String): Future[FaunaKey] = {
    val body = json.createObjectNode()
    body.put("database", database)
    body.put("role", role)
    connection.post("/keys", body) map { resp =>
      json.treeToValue(resp.resource, classOf[FaunaKey])
    }
  }

  def createIndex(indexRef: String, sourceRef: String, path: String, unique: Boolean): Future[FaunaIndex] = {
    val body = json.createObjectNode()
    body.put("source", sourceRef)
    body.put("path", path)
    body.put("unique", unique)
    connection.put(indexRef, body).map { resp =>
      json.treeToValue(resp.resource, classOf[FaunaIndex])
    }
  }

  def findInstance(instanceRef: String): Future[Option[FaunaInstance]] = {
    connection.get(instanceRef)
      .map { resp => Some(json.treeToValue(resp.resource, classOf[FaunaInstance])) }
      .recover { case _: NotFoundException => None }
  }
}