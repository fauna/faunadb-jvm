package com.faunadb.client

import _root_.java.io.FileInputStream
import _root_.java.util.{Map => JMap}
import scala.collection.JavaConverters._

import com.fasterxml.jackson.databind.ObjectMapper
import com.faunadb.client.query.Values._
import com.faunadb.client.query._
import com.faunadb.httpclient.Connection
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import org.yaml.snakeyaml.Yaml

import scala.collection.immutable
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.Random

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
    val resultFuture = rootClient.query(Create(Ref("databases"), Quote(ObjectV("name" -> testDbName))))
    val result = Await.result(resultFuture, 1 second)

    val dbRef = result.asDatabase.ref

    val keyFuture = rootClient.query(Create(Ref("keys"), Quote(ObjectV("database" -> dbRef, "role" -> "server"))))
    val key = Await.result(keyFuture, 1 second).asKey

    client = FaunaClient(Connection.builder().withFaunaRoot(config("root_url")).withAuthToken(key.secret).build())

    val classFuture = client.query(Create(Ref("classes"), Quote(ObjectV("name" -> "spells"))))
    Await.result(classFuture, 1 second)

    val indexFuture = client.query(Create(Ref("indexes"), Quote(ObjectV("name" -> "spells_by_test", "source" -> Ref("classes/spells"), "path" -> "data.queryTest1", "unique" -> false))))
    Await.result(indexFuture, 1 second)

    val setIndexFuture = client.query(Create(Ref("indexes"), Quote(ObjectV("name" -> "spells_instances", "source" -> Ref("classes/spells"), "path" -> "class", "unique" -> false))))
    Await.result(setIndexFuture, 1 second)

    val uniqueIndexFuture = client.query(Create(Ref("indexes"), Quote(ObjectV("name" -> "spells_by_unique_test", "source" -> Ref("classes/spells"), "path" -> "data.uniqueTest1", "unique" -> true))))
    Await.result(setIndexFuture, 1 second)

    val indexByElementF = client.query(Create(Ref("indexes"), Quote(ObjectV("name" -> "spells_by_element", "source" -> Ref("classes/spells"), "path" -> "data.element"))))
    Await.result(indexByElementF, 1 second)
  }

  "Fauna Client" should "should not find an instance" in {
    val resp = client.query(Get(Ref("classes/spells/1234")))
    intercept[NotFoundQueryException] {
      Await.result(resp, 1 second)
    }
  }

  it should "create a new instance" in {
    import com.faunadb.client.query.Values._
    val data = ObjectV("testField" -> "testValue")
    val respFuture = client.query(Create(Ref("classes/spells"), Quote(ObjectV("data" -> data))))
    val resp = Await.result(respFuture, 1 second).asInstance

    resp.ref.value should startWith ("classes/spells/")
    resp.classRef shouldBe Ref("classes/spells")
    resp.data("testField").asString shouldBe "testValue"
  }

  it should "create an instance with the query AST" in {
    import com.faunadb.client.query.Values._

    val queryF = client.query(Create(Ref("classes/spells"), Quote(ObjectV("data" -> ObjectV("test" -> "data")))))
    val resp = Await.result(queryF, 5 seconds).asInstance
    resp.classRef shouldBe Ref("classes/spells")
    resp.ref.value.startsWith("classes/spells") shouldBe true

    val query2F = client.query(Create(Ref("classes/spells"),
      Quote(ObjectV("data" -> ObjectV("testField" -> ObjectV("array" -> ArrayV(1, "2", 3.4), "bool" -> true, "num" -> 1234, "string" -> "sup", "float" -> 1.234, "null" -> NullV))))))
    val resp2 = Await.result(query2F, 5 seconds).asInstance
    resp2.data.contains("testField") shouldBe true
    val testFieldObject = resp2.data("testField").asObject
    testFieldObject("array").asArray(0).asNumber shouldBe 1
    testFieldObject("array").asArray(1).asString shouldBe "2"
    testFieldObject("array").asArray(2).asDouble shouldBe 3.4
    testFieldObject("string").asString shouldBe "sup"
    testFieldObject("num").asNumber shouldBe 1234
  }

  it should "issue a query with the query AST" in {
    import com.faunadb.client.query.Values._
    val randomText1 = Random.alphanumeric.take(8).mkString
    val randomText2 = Random.alphanumeric.take(8).mkString
    val classRef = Ref("classes/spells")
    val createFuture = client.query(Create(classRef, Quote(ObjectV("data" -> ObjectV("queryTest1" -> randomText1)))))
    val createFuture2 = client.query(Create(classRef, Quote(ObjectV("data" -> ObjectV("queryTest1" -> randomText2)))))

    val create1 = Await.result(createFuture, 1 second).asInstance
    val create2 = Await.result(createFuture2, 1 second).asInstance

    val queryF = client.query(Paginate(Match(randomText1, Ref("indexes/spells_by_test"))))
    val resp = Await.result(queryF, 5 seconds).asPage
    resp.data(0).asRef shouldBe create1.ref

    val query2F = client.query(Paginate(Match(Ref("classes/spells"), Ref("indexes/spells_instances"))))
    val resp2 = Await.result(query2F, 5 seconds).asPage
    val refs = resp2.data.map(_.asRef)
    refs.contains(create1.ref) shouldBe true
    refs.contains(create2.ref) shouldBe true
  }

  it should "issue a batched query" in {
    import com.faunadb.client.query.Values._
    val randomText1 = Random.alphanumeric.take(8).mkString
    val randomText2 = Random.alphanumeric.take(8).mkString
    val classRef = Ref("classes/spells")
    val expr1 = Create(classRef, Quote(ObjectV("data" -> ObjectV("queryTest1" -> randomText1))))
    val expr2 = Create(classRef, Quote(ObjectV("data" -> ObjectV("queryTest1" -> randomText2))))

    val createFuture = client.query(Seq(expr1, expr2))
    val results = Await.result(createFuture, 1 second)

    results.length shouldBe 2
    results(0).asInstance.data("queryTest1").asString shouldBe randomText1
    results(1).asInstance.data("queryTest1").asString shouldBe randomText2
  }

  it should "issue a paged query with the query AST" in {
    import com.faunadb.client.query.Values._
    val randomText1 = Random.alphanumeric.take(8).mkString
    val randomText2 = Random.alphanumeric.take(8).mkString
    val randomText3 = Random.alphanumeric.take(8).mkString
    val classRef = Ref("classes/spells")

    val createFuture = client.query(Create(classRef, Quote(ObjectV("data" -> ObjectV("queryTest1" -> randomText1)))))
    val createFuture2 = client.query(Create(classRef, Quote(ObjectV("data" -> ObjectV("queryTest1" -> randomText2)))))
    val createFuture3 = client.query(Create(classRef, Quote(ObjectV("data" -> ObjectV("queryTest1" -> randomText3)))))

    val create1 = Await.result(createFuture, 1 second).asInstance
    val create2 = Await.result(createFuture2, 1 second).asInstance
    val create3 = Await.result(createFuture3, 1 second).asInstance

    val queryF = client.query(Paginate(Match(Ref("classes/spells"), Ref("indexes/spells_instances")), size=Some(1)))
    val resp = Await.result(queryF, 5 seconds).asPage

    resp.data.size shouldBe 1
    resp.after shouldNot be (None)
    resp.before shouldBe None

    val query2F = client.query(Paginate(Match(Ref("classes/spells"), Ref("indexes/spells_instances")), size=Some(2), cursor=Some(After(resp.after.get))))
    val resp2 = Await.result(query2F, 5 seconds).asPage

    resp2.data.size shouldBe 2
    resp2.data.drop(1) shouldNot be (resp.data)
    resp2.before shouldNot be (None)
    resp2.after shouldNot be (None)
  }

  it should "issue a query with a complex expression" in {
    import com.faunadb.client.query.Values._

    val classRef = Ref("classes/spells")
    val randomText1 = Random.alphanumeric.take(8).mkString
    val randomText2 = Random.alphanumeric.take(8).mkString
    val storeOp1 = Create(classRef, Quote(ObjectV("data" -> ObjectV("test" -> randomText1))))
    val storeOp2 = Create(classRef, Quote(ObjectV("data" -> ObjectV("test" -> randomText2))))
    val queryF = client.query(Do(Array(storeOp1, storeOp2)))
    val resp = Await.result(queryF, 5 seconds).asInstance

    val query2F = client.query(Get(resp.ref))
    val resp2 = Await.result(query2F, 5 seconds).asInstance

    resp2.data("test").asString shouldBe randomText2

  }

  it should "issue a lambda query" in {
    import com.faunadb.client.query.Values._

    val randomText1 = Random.alphanumeric.take(8).mkString
    val randomText2 = Random.alphanumeric.take(8).mkString
    val classRef = Ref("classes/spells")
    val createFuture = client.query(Create(classRef, Quote(ObjectV("data" -> ObjectV("queryTest1" -> randomText1)))))
    val createFuture2 = client.query(Create(classRef, Quote(ObjectV("data" -> ObjectV("queryTest1" -> randomText2)))))

    val create1 = Await.result(createFuture, 1 second).asInstance
    val create2 = Await.result(createFuture2, 1 second).asInstance

    val queryF = client.query(com.faunadb.client.query.Map(Lambda("x", Get(Var("x"))), Paginate(Match(Ref("classes/spells"), Ref("indexes/spells_instances")), size = Some(2))))
    val resp = Await.result(queryF, 5 seconds).asPage
    resp.data.length shouldBe 2
  }

  it should "handle a constraint violation" in {
    val randomText = Random.alphanumeric.take(8).mkString
    val classRef = Ref("classes/spells")
    val createFuture = client.query(Create(classRef, Quote(ObjectV("data" -> ObjectV("uniqueTest1" -> randomText)))))
    val create1 = Await.result(createFuture, 1 second).asInstance

    val createFuture2 = client.query(Create(classRef, Quote(ObjectV("data" -> ObjectV("uniqueTest1" -> randomText)))))

    val exception = intercept[BadQueryException] {
      Await.result(createFuture2, 1 second).asInstance
    }

    exception.errors(0).code shouldBe "validation failed"
    exception.errors(0).parameters("data.uniqueTest1").error shouldBe "duplicate value"
  }

  it should "test types" in {
    val setF = client.query(Match("arcane", Ref("indexes/spells_by_element")))
    val set = Await.result(setF, 1 second).asSet
    set.parameters("match").asString shouldBe "arcane"
    set.parameters("index").asRef shouldBe Ref("indexes/spells_by_element")
  }

  it should "test basic forms" in {
    val letF = client.query(Let(immutable.Map("x" -> 1L, "y" -> 2L), Var("x")))
    val letR = Await.result(letF, 1 second)
    letR.asNumber shouldBe 1L

    val ifF = client.query(If(true, "was true", "was false"))
    val ifR = Await.result(ifF, 1 second)
    ifR.asString shouldBe "was true"

    val randomNum = Math.abs(Math.floorMod(Random.nextLong(), 250000L)) + 250000L
    val randomRef = Ref("classes/spells/" + randomNum)
    val doF = client.query(Do(Seq(
      Create(randomRef, Quote(ObjectV("data" -> ObjectV("name" -> "Magic Missile")))),
      Get(randomRef)
    )))
    val doR = Await.result(doF, 1 second).asInstance
    doR.ref shouldBe randomRef
  }
}
