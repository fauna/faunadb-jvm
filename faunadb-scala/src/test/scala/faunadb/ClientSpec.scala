package faunadb

import _root_.java.io.FileInputStream
import _root_.java.util.{Map => JMap}

import com.fasterxml.jackson.databind.ObjectMapper
import faunadb.errors.{NotFoundException, BadRequestException}
import faunadb.query.Language._
import faunadb.query._
import faunadb.types._
import com.faunadb.httpclient.Connection
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import org.yaml.snakeyaml.Yaml

import scala.collection.JavaConverters._
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
    val resultFuture = rootClient.query(Create(Ref("databases"), Quote(ObjectV("name" -> testDbName))))
    val result = Await.result(resultFuture, 1 second)

    val dbRef = result.asDatabase.ref

    val keyFuture = rootClient.query(Create(Ref("keys"), Quote(ObjectV("database" -> dbRef, "role" -> "server"))))
    val key = Await.result(keyFuture, 1 second).asKey

    client = FaunaClient(Connection.builder().withFaunaRoot(config("root_url")).withAuthToken(key.secret).build())

    val classFuture = client.query(Create(Ref("classes"), Quote(ObjectV("name" -> "spells"))))
    Await.result(classFuture, 1 second)

    val indexByElementF = client.query(Create(Ref("indexes"), Quote(ObjectV("name" -> "spells_by_element", "source" -> Ref("classes/spells"), "path" -> "data.element"))))
    Await.result(indexByElementF, 1 second)
  }

  "Fauna Client" should "should not find an instance" in {
    val resp = client.query(Get(Ref("classes/spells/1234")))
    intercept[NotFoundException] {
      Await.result(resp, 1 second)
    }
  }

  it should "create a new instance" in {
    val data = ObjectV("testField" -> "testValue")
    val respFuture = client.query(Create(Ref("classes/spells"), Quote(ObjectV("data" -> data))))
    val resp = Await.result(respFuture, 1 second).asInstance

    resp.ref.value should startWith ("classes/spells/")
    resp.classRef shouldBe Ref("classes/spells")
    resp.data("testField").asString shouldBe "testValue"

    val existsF = client.query(Exists(resp.ref))
    val existsR = Await.result(existsF, 5 seconds).asBoolean
    existsR shouldBe true

    val query2F = client.query(Create(Ref("classes/spells"),
      Quote(ObjectV("data" -> ObjectV("testField" -> ObjectV("array" -> ArrayV(1, "2", 3.4), "bool" -> true, "num" -> 1234, "string" -> "sup", "float" -> 1.234, "null" -> NullV))))))
    val resp2 = Await.result(query2F, 5 seconds).asInstance
    resp2.data.contains("testField") shouldBe true
    val testFieldObject = resp2.data("testField").asObject
    testFieldObject("array")(0).asNumber shouldBe 1
    testFieldObject("array")(1).asString shouldBe "2"
    testFieldObject("array")(2).asDouble shouldBe 3.4
    testFieldObject("string").asString shouldBe "sup"
    testFieldObject("num").asNumber shouldBe 1234
  }

  it should "issue a batched query" in {
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

  it should "issue a paginated query" in {
    val randomClassName = Random.alphanumeric.take(8).mkString
    val randomClassF = client.query(Create(Ref("classes"), Quote(ObjectV("name" -> randomClassName))))
    val classRef = Await.result(randomClassF, 1 second).asClass.ref

    val randomClassIndexF = client.query(Create(Ref("indexes"), Quote(ObjectV(
      "name" -> (randomClassName + "_class_index"),
      "source" -> classRef,
      "path" -> "class",
      "unique" -> false
    ))))

    val indexCreateF = client.query(Create(Ref("indexes"), Quote(ObjectV(
      "name" -> (randomClassName + "_test_index"),
      "source" -> classRef,
      "path" -> "data.queryTest1",
      "unique" -> false
    ))))

    val randomClassIndex = Await.result(randomClassIndexF, 5 second).asIndex.ref
    val testIndex = Await.result(indexCreateF, 5 second).asIndex.ref

    val randomText1 = Random.alphanumeric.take(8).mkString
    val randomText2 = Random.alphanumeric.take(8).mkString
    val randomText3 = Random.alphanumeric.take(8).mkString

    val createFuture = client.query(Create(classRef, Quote(ObjectV("data" -> ObjectV("queryTest1" -> randomText1)))))
    val createFuture2 = client.query(Create(classRef, Quote(ObjectV("data" -> ObjectV("queryTest1" -> randomText2)))))
    val createFuture3 = client.query(Create(classRef, Quote(ObjectV("data" -> ObjectV("queryTest1" -> randomText3)))))

    val create1 = Await.result(createFuture, 1 second).asInstance
    val create2 = Await.result(createFuture2, 1 second).asInstance
    val create3 = Await.result(createFuture3, 1 second).asInstance

    val queryMatchF = client.query(Paginate(Match(randomText1, testIndex)))
    val queryMatchR = Await.result(queryMatchF, 1 second).asPage
    queryMatchR.data.map(_.asRef) shouldBe Seq(create1.ref)

    val queryF = client.query(Paginate(Match(classRef, randomClassIndex), size=Some(1)))
    val resp = Await.result(queryF, 5 seconds).asPage

    resp.data.size shouldBe 1
    resp.after shouldNot be (None)
    resp.before shouldBe None

    val query2F = client.query(Paginate(Match(classRef, randomClassIndex), size=Some(1), cursor=Some(After(resp.after.get.asRef))))
    val resp2 = Await.result(query2F, 5 seconds).asPage

    resp2.data.size shouldBe 1
    resp2.before shouldNot be (None)
    resp2.after shouldNot be (None)

    val countF = client.query(Count(Match(classRef, randomClassIndex)))
    val countR = Await.result(countF, 1 second).asNumber
    countR shouldBe 3L
  }

  it should "handle a constraint violation" in {
    val randomClassName = Random.alphanumeric.take(8).mkString
    val randomClassF = client.query(Create(Ref("classes"), Quote(ObjectV("name" -> randomClassName))))
    val classRef = Await.result(randomClassF, 1 second).asClass.ref

    val uniqueIndexFuture = client.query(Create(Ref("indexes"), Quote(ObjectV("name" -> (randomClassName+"_by_unique_test"), "source" -> classRef, "path" -> "data.uniqueTest1", "unique" -> true))))
    Await.result(uniqueIndexFuture, 1 second)

    val randomText = Random.alphanumeric.take(8).mkString
    val createFuture = client.query(Create(classRef, Quote(ObjectV("data" -> ObjectV("uniqueTest1" -> randomText)))))
    val create1 = Await.result(createFuture, 1 second).asInstance

    val createFuture2 = client.query(Create(classRef, Quote(ObjectV("data" -> ObjectV("uniqueTest1" -> randomText)))))

    val exception = intercept[BadRequestException] {
      Await.result(createFuture2, 1 second).asInstance
    }

    exception.errors(0).code shouldBe "validation failed"
    exception.errors(0).failures.find(_.field == Seq("data", "uniqueTest1")).get.code shouldBe "duplicate value"
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

    val objectF = client.query(Quote(ObjectV("name" -> "Hen Wen", "age" -> 123L)))
    val objectR = Await.result(objectF, 1 second).asObject
    objectR("name").asString shouldBe "Hen Wen"
    objectR("age").asNumber shouldBe 123L

  }

  it should "test collections" in {
    val mapF = client.query(Map(Lambda("munchings", Add(Seq(Var("munchings"), 1L))), ArrayV(1L, 2L, 3L)))
    val mapR = Await.result(mapF, 1 second).asArray
    mapR.toSeq.map(_.asNumber) shouldBe Seq(2L, 3L, 4L)

    val foreachF = client.query(Foreach(Lambda("spell", Create(Ref("classes/spells"), Object(ObjectV("data" -> Object(ObjectV("name" -> Var("spell"))))))), ArrayV("Fireball Level 1", "Fireball Level 2")))
    val foreachR = Await.result(foreachF, 1 second).asArray
    foreachR.toSeq.map(_.asString) shouldBe Seq("Fireball Level 1", "Fireball Level 2")
  }

  it should "test resource modification" in {
    val createF = client.query(Create(Ref("classes/spells"), Quote(ObjectV("data" -> ObjectV("name" -> "Magic Missile", "element" -> "arcane", "cost" -> 10L)))))
    val createR = Await.result(createF, 1 second).asInstance
    createR.ref.value should startWith("classes/spells")
    createR.data("name").asString shouldBe "Magic Missile"
    createR.data("element").asString shouldBe "arcane"
    createR.data("cost").asNumber shouldBe 10L

    val updateF = client.query(Update(createR.ref, Quote(ObjectV("data" -> ObjectV("name" -> "Faerie Fire", "cost" -> NullV)))))
    val updateR = Await.result(updateF, 1 second).asInstance
    updateR.ref shouldBe createR.ref
    updateR.data("name").asString shouldBe "Faerie Fire"
    updateR.data("element").asString shouldBe "arcane"
    updateR.data.get("cost") shouldBe None

    val replaceF = client.query(Replace(createR.ref, Quote(ObjectV("data" -> ObjectV("name" -> "Volcano", "element" -> ArrayV("fire", "earth"), "cost" -> 10L)))))
    val replaceR = Await.result(replaceF, 1 second).asInstance
    replaceR.ref shouldBe createR.ref
    replaceR.data("name").asString shouldBe "Volcano"
    replaceR.data("element").asArray.toSeq.map(_.asString) shouldBe Seq("fire", "earth")
    replaceR.data("cost").asNumber shouldBe 10L

    val deleteF = client.query(Delete(createR.ref))
    Await.result(deleteF, 1 second)
    val getF = client.query(Get(createR.ref))
    intercept[NotFoundException] {
      Await.result(getF, 1 second)
    }
  }

  it should "test sets" in {
    val create1F = client.query(Create(Ref("classes/spells"), Quote(ObjectV("data" -> ObjectV("name" -> "Magic Missile", "element" -> "arcane", "cost" -> 10L)))))
    val create2F = client.query(Create(Ref("classes/spells"), Quote(ObjectV("data" -> ObjectV("name" -> "Fireball", "element" -> "fire", "cost" -> 10L)))))
    val create3F = client.query(Create(Ref("classes/spells"), Quote(ObjectV("data" -> ObjectV("name" -> "Faerie Fire", "element" -> ArrayV("arcane", "nature"), "cost" -> 10L)))))
    val create4F = client.query(Create(Ref("classes/spells"), Quote(ObjectV("data" -> ObjectV("name" -> "Summon Animal Companion", "element" -> "nature", "cost" -> 10L)))))
    val create1R = Await.result(create1F, 1 second).asInstance
    val create2R = Await.result(create2F, 1 second).asInstance
    val create3R = Await.result(create3F, 1 second).asInstance
    val create4R = Await.result(create4F, 1 second).asInstance

    val matchF = client.query(Paginate(Match("arcane", Ref("indexes/spells_by_element"))))
    val matchR = Await.result(matchF, 1 second).asPage
    matchR.data.map(_.asRef) should contain (create1R.ref)

    val matchEventsF = client.query(Paginate(Match("arcane", Ref("indexes/spells_by_element")), events=true))
    val matchEventsR = Await.result(matchEventsF, 1 second).asPage
    matchEventsR.data.map(_.asEvent.resource) should contain (create1R.ref)

    val unionF = client.query(Paginate(Union(Seq(Match("arcane", Ref("indexes/spells_by_element")), Match("fire", Ref("indexes/spells_by_element"))))))
    val unionR = Await.result(unionF, 1 second).asPage
    unionR.data.map(_.asRef) should (contain (create1R.ref) and contain (create2R.ref))

    val unionEventsF = client.query(Paginate(Union(Seq(Match("arcane", Ref("indexes/spells_by_element")), Match("fire", Ref("indexes/spells_by_element")))), events=true))
    val unionEventsR = Await.result(unionEventsF, 1 second).asPage
    unionEventsR.data.map(_.asEvent).filter(_.action == "create").map(_.resource) should (contain (create1R.ref) and contain (create2R.ref))

    val intersectionF = client.query(Paginate(Intersection(Seq(Match("arcane", Ref("indexes/spells_by_element")), Match("nature", Ref("indexes/spells_by_element"))))))
    val intersectionR = Await.result(intersectionF, 1 second).asPage
    intersectionR.data.map(_.asRef) should contain (create3R.ref)

    val differenceF = client.query(Paginate(Difference(Seq(Match("nature", Ref("indexes/spells_by_element")), Match("arcane", Ref("indexes/spells_by_element"))))))
    val differenceR = Await.result(differenceF, 1 second).asPage
    differenceR.data.map(_.asRef) should contain (create4R.ref)
  }

  it should "test miscellaneous functions" in {
    val equalsF = client.query(Equals(Seq("fire", "fire")))
    val equalsR = Await.result(equalsF, 1 second).asBoolean
    equalsR shouldBe true

    val concatF = client.query(Concat(Seq("Magic", "Missile")))
    val concatR = Await.result(concatF, 1 second).asString
    concatR shouldBe "MagicMissile"

    val containsF = client.query(Contains(Seq("favorites", "foods"), Quote(ObjectV("favorites" -> ObjectV("foods" -> ArrayV("crunchings", "munchings"))))))
    val containsR = Await.result(containsF, 1 second).asBoolean
    containsR shouldBe true

    val selectF = client.query(Select(Seq("favorites", "foods", 1), Quote(ObjectV("favorites" -> ObjectV("foods" -> ArrayV("crunchings", "munchings", "lunchings"))))))
    val selectR = Await.result(selectF, 1 second).asString
    selectR shouldBe "munchings"

    val addF = client.query(Add(Seq(100L, 10L)))
    val addR = Await.result(addF, 1 second).asNumber
    addR shouldBe 110L

    val multiplyF = client.query(Multiply(Seq(100L, 10L)))
    val multiplyR = Await.result(multiplyF, 1 second).asNumber
    multiplyR shouldBe 1000L

    val subtractF = client.query(Subtract(Seq(100L, 10L)))
    val subtractR = Await.result(subtractF, 1 second).asNumber
    subtractR shouldBe 90L

    val divideF = client.query(Divide(Seq(100L, 10L)))
    val divideR = Await.result(divideF, 1 second).asNumber
    divideR shouldBe 10L
  }
}
