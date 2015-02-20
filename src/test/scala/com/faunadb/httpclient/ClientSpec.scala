package com.faunadb.httpclient

import java.io.FileInputStream
import java.util.{Map => JMap}

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.faunadb.FaunaInstance
import com.faunadb.query._
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import org.yaml.snakeyaml.Yaml

import scala.collection.JavaConverters._
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random

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

    val classFuture = client.createClass("classes/spells")
    Await.result(classFuture, 1 second)

    val indexFuture = client.createIndex("indexes/spells_by_test", "classes/spells", "data.queryTest1", false)
    Await.result(indexFuture, 1 second)
  }

  "Fauna Client" should "should not find an instance" in {
    val resp = client.findInstance("classes/spells/1234")
    Await.result(resp, 1 second) shouldBe None
  }

  it should "create a new instance" in {
    val data = Map("testField" -> "testValue").asJava
    val dataNode: ObjectNode = json.valueToTree(data)
    val instanceToCreate = new FaunaInstance(classRef="classes/spells", data=dataNode)
    val respFuture = client.createOrReplaceInstance(instanceToCreate)
    val resp = Await.result(respFuture, 1 second)

    resp.ref should startWith ("classes/spells/")
    resp.classRef shouldBe "classes/spells"
    json.treeToValue(resp.data, classOf[JMap[String, String]]) shouldBe data
  }

  it should "create an instance with the query AST" in {
    import Primitives._

    val queryF = client.query[InstanceResponse](Create(Ref("classes/spells"), ObjectPrimitive(Map[String, Primitive]("data" -> Map[String, Primitive]("test" -> "data")))))
    val resp = Await.result(queryF, 5 seconds)
    resp.classRef shouldBe Ref("classes/spells")
    resp.ref.ref.startsWith("classes/spells") shouldBe true
  }

  it should "issue a query with the query AST" in {
    import Primitives._
    val randomText1 = Random.alphanumeric.take(8).mkString
    val randomText2 = Random.alphanumeric.take(8).mkString
    val classRef = Ref("classes/spells")
    val createFuture = client.query[InstanceResponse](Create(classRef, Map[String, Primitive]("data" -> Map[String, Primitive]("queryTest1" -> randomText1))))
    val createFuture2 = client.query[InstanceResponse](Create(classRef, Map[String, Primitive]("data" -> Map[String, Primitive]("queryTest1" -> randomText2))))

    val create1 = Await.result(createFuture, 1 second)
    val create2 = Await.result(createFuture2, 1 second)

    val queryF = client.query[SetResponse](Get(Match(randomText1, Ref("indexes/spells_by_test"))))
    val resp = Await.result(queryF, 5 seconds)
    resp.data.map { _.value } shouldBe Seq(create1.ref)
  }

  it should "issue a query with a complex expression" in {
    import Primitives._

    val classRef = Ref("classes/spells")
    val randomText1 = Random.alphanumeric.take(8).mkString
    val randomText2 = Random.alphanumeric.take(8).mkString
    val storeOp1 = Create(classRef, Map[String, Primitive]("data" -> Map[String, Primitive]("test" -> randomText1)))
    val storeOp2 = Create(classRef, Map[String, Primitive]("data" -> Map[String, Primitive]("test" -> randomText2)))
    val queryF = client.query[InstanceResponse](Do(Array(storeOp1, storeOp2)))
    val resp = Await.result(queryF, 5 seconds)

    val query2F = client.query[InstanceResponse](Get(resp.ref))
    val resp2 = Await.result(query2F, 5 seconds)

    resp2.data.get("test") shouldBe randomText2
  }
}
