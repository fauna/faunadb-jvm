package com.faunadb.httpclient

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.{ArrayNode, ObjectNode}
import com.faunadb._
import scala.collection.JavaConversions._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class FaunaClient(connection: Connection) {
  private val json = new ObjectMapper

  def query(query: String): Future[FaunaSet] = {
    connection.get("/queries", "q" -> query).map { resourceToSet(_) }
  }

  private def postOrPut(instance: FaunaInstance) = {
    val body = json.createObjectNode()
    body.set("data", instance.data)
    body.set("constraints", instance.constraints)
    if (instance.ref.isEmpty) {
      val uri = "/" + instance.classRef
      connection.post(uri, body)
    } else {
      val uri = "/" + instance.ref
      connection.put(uri, body)
    }
  }

  def createOrReplaceInstance(instance: FaunaInstance): Future[FaunaInstance] = {
    postOrPut(instance) map { resp =>
      json.treeToValue(resp.resource, classOf[FaunaInstance])
    }
  }

  def createOrPatchInstance(instance: FaunaInstance): Future[FaunaInstance] = {
    val body = json.createObjectNode()
    body.set("data", instance.data)
    body.set("constraints", instance.constraints)
    val rv = if (instance.ref.isEmpty) {
      val uri = "/" + instance.classRef
      connection.post(uri, body)
    } else {
      val uri = "/" + instance.ref
      connection.patch(uri, body)
    }

    rv map { resp => json.treeToValue(resp.resource, classOf[FaunaInstance]) }
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

  def findInstance(instanceRef: String): Future[FaunaInstance] = {
    connection.get(instanceRef).map { resp => json.treeToValue(resp.resource, classOf[FaunaInstance]) }
  }

  private def resourceToSet(resp: ResourceResponse): FaunaSet  = {
    val resources = resp.resource.asInstanceOf[ObjectNode].get("resources")
    resp.resource.asInstanceOf[ObjectNode].remove("resources")
    val rv = json.treeToValue(resp.resource, classOf[FaunaSet])

    if (resources.isArray && resp.references.size() > 0) {
      val resArray = resources.asInstanceOf[ArrayNode]
      resArray foreach { ref =>
        val refStr = ref.asText()
        rv.resources += (refStr -> json.treeToValue(resp.references.get(refStr), classOf[FaunaInstance]))
      }
    }

    rv
  }
}