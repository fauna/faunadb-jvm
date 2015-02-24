package com.faunadb.httpclient

import java.io.FileInputStream
import java.util.{Map => JMap}

import com.fasterxml.jackson.databind.ObjectMapper
import com.faunadb.query._
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import org.yaml.snakeyaml.Yaml

import scala.collection.JavaConverters._
import scala.collection.Map
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

    val setIndexFuture = client.createIndex("indexes/spells_instances", "classes/spells", "class", false)
    Await.result(setIndexFuture, 1 second)
  }

  "Fauna Client" should "should not find an instance" in {
    val resp = client.findInstance("classes/spells/1234")
    Await.result(resp, 1 second) shouldBe None
  }

  it should "create a new instance" in {
    import com.faunadb.query.Primitives._
    val data = Map[String, Primitive]("testField" -> "testValue")
    val respFuture = client.query[InstanceResponse](Create(Ref("classes/spells"), Map[String, Primitive]("data" -> data)))
    val resp = Await.result(respFuture, 1 second)

    resp.ref.ref should startWith ("classes/spells/")
    resp.classRef shouldBe Ref("classes/spells")
    resp.data.values shouldBe data
  }

  it should "create an instance with the query AST" in {
    import com.faunadb.query.Primitives._

    val queryF = client.query[InstanceResponse](Create(Ref("classes/spells"), Map[String, Primitive]("data" -> Map[String, Primitive]("test" -> "data"))))
    val resp = Await.result(queryF, 5 seconds)
    resp.classRef shouldBe Ref("classes/spells")
    resp.ref.ref.startsWith("classes/spells") shouldBe true

    val query2F = client.query[InstanceResponse](Create(Ref("classes/spells"),
      Map[String, Primitive]("data" -> Map[String, Primitive]("testField" -> Map[String, Primitive]("array" -> Array[Primitive](1, "2", 3.4), "bool" -> true, "num" -> 1234, "string" -> "sup", "float" -> 1.234, "null" -> NullPrimitive)))))
    val resp2 = Await.result(query2F, 5 seconds)
    resp2.data.values.contains("testField") shouldBe true
    val testFieldObject = resp2.data.values("testField").asObject
    testFieldObject.values("array").asArray.values.toSeq shouldBe Array[Primitive](1, "2", 3.4).toSeq
    testFieldObject.values("string").asString.value shouldBe "sup"
    testFieldObject.values("num").asNumber.value shouldBe 1234
  }

  it should "issue a query with the query AST" in {
    import com.faunadb.query.Primitives._
    val randomText1 = Random.alphanumeric.take(8).mkString
    val randomText2 = Random.alphanumeric.take(8).mkString
    val classRef = Ref("classes/spells")
    val createFuture = client.query[InstanceResponse](Create(classRef, Map[String, Primitive]("data" -> Map[String, Primitive]("queryTest1" -> randomText1))))
    val createFuture2 = client.query[InstanceResponse](Create(classRef, Map[String, Primitive]("data" -> Map[String, Primitive]("queryTest1" -> randomText2))))

    val create1 = Await.result(createFuture, 1 second)
    val create2 = Await.result(createFuture2, 1 second)

    val queryF = client.query[SetResponse](Paginate(Match(randomText1, Ref("indexes/spells_by_test"))))
    val resp = Await.result(queryF, 5 seconds)
    resp.data shouldBe Seq(create1.ref)

    val query2F = client.query[SetResponse](Paginate(Ref("classes/spells/instances")))
    val resp2 = Await.result(query2F, 5 seconds)
    resp2.data.toList.contains(create1.ref) shouldBe true
    resp2.data.toList.contains(create2.ref) shouldBe true
  }

  it should "issue a paged query with the query AST" in {
    import com.faunadb.query.Primitives._
    val randomText1 = Random.alphanumeric.take(8).mkString
    val randomText2 = Random.alphanumeric.take(8).mkString
    val randomText3 = Random.alphanumeric.take(8).mkString
    val classRef = Ref("classes/spells")

    val createFuture = client.query[InstanceResponse](Create(classRef, Map[String, Primitive]("data" -> Map[String, Primitive]("queryTest1" -> randomText1))))
    val createFuture2 = client.query[InstanceResponse](Create(classRef, Map[String, Primitive]("data" -> Map[String, Primitive]("queryTest1" -> randomText2))))
    val createFuture3 = client.query[InstanceResponse](Create(classRef, Map[String, Primitive]("data" -> Map[String, Primitive]("queryTest1" -> randomText3))))

    val create1 = Await.result(createFuture, 1 second)
    val create2 = Await.result(createFuture2, 1 second)
    val create3 = Await.result(createFuture3, 1 second)

    val queryF = client.query[SetResponse](Paginate(Ref("classes/spells/instances"), size=Some(1)))
    val resp = Await.result(queryF, 5 seconds)

    resp.data.size shouldBe 1
    resp.before shouldNot be (None)
    resp.after shouldBe None

    val query2F = client.query[SetResponse](Paginate(Ref("classes/spells/instances"), size=Some(1), cursor=Some(com.faunadb.query.Before(resp.before.get))))
    val resp2 = Await.result(query2F, 5 seconds)

    resp2.data.size shouldBe 1
    resp2.data shouldNot be (resp.data)
    resp2.before shouldNot be (None)
    resp2.after shouldNot be (None)
  }

  it should "issue a query with a complex expression" in {
    import com.faunadb.query.Primitives._

    val classRef = Ref("classes/spells")
    val randomText1 = Random.alphanumeric.take(8).mkString
    val randomText2 = Random.alphanumeric.take(8).mkString
    val storeOp1 = Create(classRef, Map[String, Primitive]("data" -> Map[String, Primitive]("test" -> randomText1)))
    val storeOp2 = Create(classRef, Map[String, Primitive]("data" -> Map[String, Primitive]("test" -> randomText2)))
    val queryF = client.query[InstanceResponse](Do(Array(storeOp1, storeOp2)))
    val resp = Await.result(queryF, 5 seconds)

    val query2F = client.query[InstanceResponse](Get(resp.ref))
    val resp2 = Await.result(query2F, 5 seconds)

    resp2.data.values.get("test").get shouldBe StringPrimitive(randomText2)
  }
}
