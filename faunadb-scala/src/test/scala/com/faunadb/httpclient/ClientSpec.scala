package com.faunadb.httpclient

import java.io.FileInputStream
import java.util.{Map => JMap}

import com.fasterxml.jackson.databind.ObjectMapper
import com.faunadb.client.{NotFoundQueryException, FaunaClient}
import com.faunadb.client.query._
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import org.yaml.snakeyaml.Yaml

import scala.collection.JavaConverters._
import scala.collection.Map
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Random
import scala.concurrent.ExecutionContext.Implicits.global

class ClientSpec extends FlatSpec with Matchers with BeforeAndAfterAll {
  val config = readConfig("config/test.yml")
  val json = new ObjectMapper()

  val rootClient = FaunaClient(Connection.builder().withFaunaRoot(config("root_url")).withAuthToken(config("root_token")).build())

  val testDbName = "fauna-java-test-" + Random.alphanumeric.take(8).mkString
  var client: FaunaClient = null

  private def readConfig(filename: String) = {
    val reader = new FileInputStream(filename)
    val rv = new Yaml().loadAs(reader, classOf[JMap[String, String]])
    reader.close()
    rv.asScala
  }

  override protected def beforeAll(): Unit = {
    import Values._
    val resultFuture = rootClient.query[Database](Create(Ref("databases"), ObjectV("name" -> testDbName)))
    val result = Await.result(resultFuture, 1 second)

    val dbRef = result.ref

    val keyFuture = rootClient.query[Key](Create(Ref("keys"), ObjectV("database" -> dbRef, "role" -> "server")))
    val key = Await.result(keyFuture, 1 second)

    client = FaunaClient(Connection.builder().withFaunaRoot(config("root_url")).withAuthToken(key.secret).build())

    val classFuture = client.query[Class](Create(Ref("classes"), ObjectV("name" -> "spells")))
    Await.result(classFuture, 1 second)

    val indexFuture = client.query[Index](Create(Ref("indexes"), ObjectV("name" -> "spells_by_test", "source" -> Ref("classes/spells"), "path" -> "data.queryTest1", "unique" -> false)))
    Await.result(indexFuture, 1 second)

    val setIndexFuture = client.query[Index](Create(Ref("indexes"), ObjectV("name" -> "spells_instances", "source" -> Ref("classes/spells"), "path" -> "class", "unique" -> false)))
    Await.result(setIndexFuture, 1 second)
  }

  "Fauna Client" should "should not find an instance" in {
    val resp = client.query[Instance](Get(Ref("classes/spells/1234")))
    intercept[NotFoundQueryException] {
      Await.result(resp, 1 second)
    }
  }

  it should "create a new instance" in {
    import com.faunadb.client.query.Values._
    val data = ObjectV("testField" -> "testValue")
    val respFuture = client.query[Instance](Create(Ref("classes/spells"), ObjectV("data" -> data)))
    val resp = Await.result(respFuture, 1 second)

    resp.ref.value should startWith ("classes/spells/")
    resp.classRef shouldBe Ref("classes/spells")
    resp.data shouldBe data
  }

  it should "create an instance with the query AST" in {
    import com.faunadb.client.query.Values._

    val queryF = client.query[Instance](Create(Ref("classes/spells"), ObjectV("data" -> ObjectV("test" -> "data"))))
    val resp = Await.result(queryF, 5 seconds)
    resp.classRef shouldBe Ref("classes/spells")
    resp.ref.value.startsWith("classes/spells") shouldBe true

    val query2F = client.query[Instance](Create(Ref("classes/spells"),
      ObjectV("data" -> ObjectV("testField" -> ObjectV("array" -> ArrayV(1, "2", 3.4), "bool" -> true, "num" -> 1234, "string" -> "sup", "float" -> 1.234, "null" -> NullPrimitive)))))
    val resp2 = Await.result(query2F, 5 seconds)
    resp2.data.values.contains("testField") shouldBe true
    val testFieldObject = resp2.data.values("testField").asObject
    testFieldObject("array").asArray.toSeq shouldBe Seq[Value](1, "2", 3.4)
    testFieldObject("string").asString shouldBe "sup"
    testFieldObject("num").asNumber shouldBe 1234
  }

  it should "issue a query with the query AST" in {
    import com.faunadb.client.query.Values._
    val randomText1 = Random.alphanumeric.take(8).mkString
    val randomText2 = Random.alphanumeric.take(8).mkString
    val classRef = Ref("classes/spells")
    val createFuture = client.query[Instance](Create(classRef, ObjectV("data" -> ObjectV("queryTest1" -> randomText1))))
    val createFuture2 = client.query[Instance](Create(classRef, ObjectV("data" -> ObjectV("queryTest1" -> randomText2))))

    val create1 = Await.result(createFuture, 1 second)
    val create2 = Await.result(createFuture2, 1 second)

    val queryF = client.querySet[Ref](Paginate(Match(randomText1, Ref("indexes/spells_by_test"))))
    val resp = Await.result(queryF, 5 seconds)
    resp.data shouldBe Seq(create1.ref)

    val query2F = client.querySet[Ref](Paginate(Match(Ref("classes/spells"), Ref("indexes/spells_instances"))))
    val resp2 = Await.result(query2F, 5 seconds)
    resp2.data.toList.contains(create1.ref) shouldBe true
    resp2.data.toList.contains(create2.ref) shouldBe true
  }

  it should "issue a paged query with the query AST" in {
    import com.faunadb.client.query.Values._
    val randomText1 = Random.alphanumeric.take(8).mkString
    val randomText2 = Random.alphanumeric.take(8).mkString
    val randomText3 = Random.alphanumeric.take(8).mkString
    val classRef = Ref("classes/spells")

    val createFuture = client.query[Instance](Create(classRef, ObjectV("data" -> ObjectV("queryTest1" -> randomText1))))
    val createFuture2 = client.query[Instance](Create(classRef, ObjectV("data" -> ObjectV("queryTest1" -> randomText2))))
    val createFuture3 = client.query[Instance](Create(classRef, ObjectV("data" -> ObjectV("queryTest1" -> randomText3))))

    val create1 = Await.result(createFuture, 1 second)
    val create2 = Await.result(createFuture2, 1 second)
    val create3 = Await.result(createFuture3, 1 second)

    val queryF = client.querySet[Ref](Paginate(Match(Ref("classes/spells"), Ref("indexes/spells_instances")), size=Some(1)))
    val resp = Await.result(queryF, 5 seconds)

    resp.data.size shouldBe 1
    resp.before shouldNot be (None)
    resp.after shouldBe None

    val query2F = client.querySet[Ref](Paginate(Match(Ref("classes/spells"), Ref("indexes/spells_instances")), size=Some(1), cursor=Some(Before(resp.before.get))))
    val resp2 = Await.result(query2F, 5 seconds)

    resp2.data.size shouldBe 1
    resp2.data shouldNot be (resp.data)
    resp2.before shouldNot be (None)
    resp2.after shouldNot be (None)
  }

  it should "issue a query with a complex expression" in {
    import com.faunadb.client.query.Values._

    val classRef = Ref("classes/spells")
    val randomText1 = Random.alphanumeric.take(8).mkString
    val randomText2 = Random.alphanumeric.take(8).mkString
    val storeOp1 = Create(classRef, ObjectV("data" -> ObjectV("test" -> randomText1)))
    val storeOp2 = Create(classRef, ObjectV("data" -> ObjectV("test" -> randomText2)))
    val queryF = client.query[Instance](Do(Array(storeOp1, storeOp2)))
    val resp = Await.result(queryF, 5 seconds)

    val query2F = client.query[Instance](Get(resp.ref))
    val resp2 = Await.result(query2F, 5 seconds)

    resp2.data.values.get("test").get shouldBe StringV(randomText2)

  }

  it should "issue a lambda query" in {
    import com.faunadb.client.query.Values._

    val randomText1 = Random.alphanumeric.take(8).mkString
    val randomText2 = Random.alphanumeric.take(8).mkString
    val classRef = Ref("classes/spells")
    val createFuture = client.query[Instance](Create(classRef, ObjectV("data" -> ObjectV("queryTest1" -> randomText1))))
    val createFuture2 = client.query[Instance](Create(classRef, ObjectV("data" -> ObjectV("queryTest1" -> randomText2))))

    val create1 = Await.result(createFuture, 1 second)
    val create2 = Await.result(createFuture2, 1 second)

    val queryF = client.querySet[Instance](com.faunadb.client.query.Map(Lambda("x", Get(Var("x"))), Paginate(Match(Ref("classes/spells"), Ref("indexes/spells_instances")), size = Some(2))))
    val resp = Await.result(queryF, 5 seconds)
    resp.data.length shouldBe 2
  }
}
