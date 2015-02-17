package com.faunadb.httpclient

import java.io.FileInputStream

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.faunadb.FaunaInstance
import com.faunadb.query.Terms._
import org.scalatest.{BeforeAndAfterAll, Matchers, FlatSpec}
import org.yaml.snakeyaml.Yaml

import scala.collection.JavaConverters._
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._

import java.util.{ Map => JMap }

import scala.util.Random
import scala.concurrent.ExecutionContext.Implicits.global

class ClientSpec extends FlatSpec with Matchers with BeforeAndAfterAll {
  val config = readConfig("config/test.yml")
  val json = new ObjectMapper()

  val rootClient = new FaunaClient(Connection.Builder().setFaunaRoot(config("root_url")).setAuthToken(config("root_token")).build())

  val testDbName = "fauna-java-test-" + Random.alphanumeric.take(8).mkString
  var client: FaunaClient = null

  private def readConfig(filename: String) = {
    val reader = new FileInputStream(filename)
    val rv = new Yaml().loadAs(reader, classOf[JMap[String, String]])
    reader.close()
    rv.asScala
  }

  override protected def beforeAll(): Unit = {
    val resultFuture = rootClient.createDatabase(testDbName)
    val result = Await.result(resultFuture, 1 second)

    val dbRef = result.ref

    val keyFuture = rootClient.createKey(dbRef, "server")
    val key = Await.result(keyFuture, 1 second)

    client = new FaunaClient(Connection.Builder().setFaunaRoot(config("root_url")).setAuthToken(key.secret).build())

    val classFuture = client.createClass("classes/derp")
    Await.result(classFuture, 1 second)

    val indexFuture = client.createIndex("indexes/derp_by_test", "classes/derp", "data.queryTest1", false)
    Await.result(indexFuture, 1 second)
  }


  "Fauna Client" should "should not find an instance" in {
    val resp = client.findInstance("classes/derp/1234")
    Await.result(resp, 1 second) shouldBe None
  }

  it should "create a new instance" in {
    val data = Map("testField" -> "testValue").asJava
    val dataNode: ObjectNode = json.valueToTree(data)
    val instanceToCreate = new FaunaInstance(classRef="classes/derp", data=dataNode)
    val respFuture = client.createOrReplaceInstance(instanceToCreate)
    val resp = Await.result(respFuture, 1 second)

    resp.ref should startWith ("classes/derp/")
    resp.classRef shouldBe "classes/derp"
    json.treeToValue(resp.data, classOf[JMap[String, String]]) shouldBe data
  }

  it should "successfully issue a query" in {
    val randomText1 = Random.alphanumeric.take(8).mkString
    val randomText2 = Random.alphanumeric.take(8).mkString
    val instance1 = FaunaInstance(classRef="classes/derp", data=json.valueToTree[ObjectNode](Map("queryTest1" -> randomText1).asJava))
    val instance2 = FaunaInstance(classRef="classes/derp", data=json.valueToTree[ObjectNode](Map("queryTest1" -> randomText2).asJava))

    val createFuture = client.createOrReplaceInstance(instance1)
    val createFuture2 = client.createOrReplaceInstance(instance2)

    val create1 = Await.result(createFuture, 1 second)
    val create2 = Await.result(createFuture2, 1 second)

    val queryFuture = client.query(Match("indexes/derp_by_test", randomText1).toQueryString)
    val queryResult = Await.result(queryFuture, 1 second)
    queryResult.resources.keys.toSeq shouldBe Seq(create1.ref)
  }

  it should "get and page a query" in {
    val futures = for(i <- 1.until(10)) yield {
      val instance = FaunaInstance(classRef="users")
      client.createOrReplaceInstance(instance)
    }

    Await.result(Future.sequence(futures), 5 seconds)

    val fullQuery = Await.result(client.query("users/instances"), 5 seconds)
    val singleQuery = Await.result(client.query("users/instances", 1), 5 seconds)

    singleQuery.resources.size shouldBe 1
    singleQuery.resources.head._1 shouldBe fullQuery.resources.head._1

    val nextQuery = Await.result(client.query("users/instances", 1, Before(singleQuery.before.get)), 5 seconds)
    nextQuery.resources.size shouldBe 1
    nextQuery.resources.head._1 shouldBe fullQuery.resources.tail.head._1
  }
}
