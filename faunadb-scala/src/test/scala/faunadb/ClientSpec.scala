package faunadb

import com.faunadb.httpclient.Connection
import faunadb.errors.{UnauthorizedException, NotFoundException, BadRequestException}
import faunadb.query._
import faunadb.values._
import java.time.temporal.ChronoUnit
import java.time.{LocalDate, Instant}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}
import scala.concurrent.{ Await, Future }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.util.Random

class ClientSpec extends FlatSpec with Matchers with BeforeAndAfterAll {
  val config = {
    val rootKey = Option(System.getenv("FAUNA_ROOT_KEY")) getOrElse {
      throw new RuntimeException("FAUNA_ROOT_KEY must defined to run tests")
    }
    val domain = Option(System.getenv("FAUNA_DOMAIN")) getOrElse { "rest.faunadb.com" }
    val scheme = Option(System.getenv("FAUNA_SCHEME")) getOrElse { "https" }
    val port = Option(System.getenv("FAUNA_PORT")) getOrElse { "443" }

    collection.Map("root_token" -> rootKey, "root_url" -> s"${scheme}://${domain}:${port}")
  }

  val rootClient = FaunaClient(config("root_url"), secret = config("root_token"))

  val testDbName = "faunadb-scala-test-" + Random.alphanumeric.take(8).mkString
  var client: FaunaClient = null

  // Helper fields

  val RefField = Field("ref").as[Ref]
  val ClassField = Field("class").as[Ref]
  val SecretField = Field("secret").as[String]

  // Page helpers
  case class Ev(ref: Ref, ts: Long, action: String)

  val EventField = Field.zip(
    Field("resource").as[Ref],
    Field("ts").as[Long],
    Field("action").as[String]
  ) map { case (r, ts, a) => Ev(r, ts, a) }

  val PageEvents = Field("data").collect(EventField)
  val PageRefs = Field("data").as[Seq[Ref]]

  def await[T](f: Future[T]) = Await.result(f, 1.second)

  // tests

  override protected def beforeAll(): Unit = {
    val db = await(rootClient.query(Create(Ref("databases"), Obj("name" -> testDbName))))
    val dbRef = db(RefField).get
    val key = await(rootClient.query(Create(Ref("keys"), Obj("database" -> dbRef, "role" -> "server"))))

    client = FaunaClient(config("root_url"), secret = key(SecretField).get)

    await(client.query(Create(Ref("classes"), Obj("name" -> "spells"))))

    await(client.query(Create(Ref("indexes"), Obj(
      "name" -> "spells_by_element",
      "source" -> Ref("classes/spells"),
      "terms" -> Arr(Obj("path" -> "data.element")),
      "active" -> true))))
  }

  override protected def afterAll(): Unit = {
    client.close()
  }

  "Fauna Client" should "should not find an instance" in {
    a[NotFoundException] should be thrownBy (await(client.query(Get(Ref("classes/spells/1234")))))
  }

  it should "echo values" in {
    await(client.query(ObjectV("foo" -> StringV("bar")))) should equal (ObjectV("foo" -> StringV("bar")))
    await(client.query("qux")) should equal (StringV("qux"))
  }

  it should "fail with unauthorized" in {
    val badClient = FaunaClient(config("root_url"), secret = "notavalidsecret")
    an[UnauthorizedException] should be thrownBy (await(badClient.query(Get(Ref("classes/spells/12345")))))
  }

  it should "create a new instance" in {
    val inst = await(client.query(
      Create(Ref("classes/spells"),
        Obj("data" -> Obj("testField" -> "testValue")))))

    inst(RefField).get.value should startWith ("classes/spells/")
    inst(ClassField).get should equal (Ref("classes/spells"))
    inst("data", "testField").as[String].get should equal ("testValue")

    await(client.query(Exists(inst(RefField)))) should equal (TrueV)

    val inst2 = await(client.query(Create(Ref("classes/spells"),
      Obj("data" -> Obj(
        "testData" -> Obj(
          "array" -> Arr(1, "2", 3.4),
          "bool" -> true,
          "num" -> 1234,
          "string" -> "sup",
          "float" -> 1.234,
          "null" -> NullV))))))

    val testData = inst2("data", "testData")

    testData.isDefined shouldBe true
    testData("array", 0).as[Long].get shouldBe 1
    testData("array", 1).as[String].get shouldBe "2"
    testData("array", 2).as[Double].get shouldBe 3.4
    testData("string").as[String].get shouldBe "sup"
    testData( "num").as[Long].get shouldBe 1234
  }

  it should "issue a batched query" in {
    val randomText1 = Random.alphanumeric.take(8).mkString
    val randomText2 = Random.alphanumeric.take(8).mkString
    val classRef = Ref("classes/spells")
    val expr1 = Create(classRef, Obj("data" -> Obj("queryTest1" -> randomText1)))
    val expr2 = Create(classRef, Obj("data" -> Obj("queryTest1" -> randomText2)))

    val results = await(client.query(Seq(expr1, expr2)))

    results.length shouldBe 2
    results(0)("data", "queryTest1").as[String].get shouldBe randomText1
    results(1)("data", "queryTest1").as[String].get shouldBe randomText2
  }

  it should "issue a paginated query" in {
    val randomClassName = Random.alphanumeric.take(8).mkString
    val randomClassF = client.query(Create(Ref("classes"), Obj("name" -> randomClassName)))
    val classRef = Await.result(randomClassF, 1 second)(RefField).get

    val randomClassIndexF = client.query(Create(Ref("indexes"), Obj(
      "name" -> (randomClassName + "_class_index"),
      "source" -> classRef,
      "active" -> true,
      "unique" -> false
    )))

    val indexCreateF = client.query(Create(Ref("indexes"), Obj(
      "name" -> (randomClassName + "_test_index"),
      "source" -> classRef,
      "terms" -> Arr(Obj("path" -> "data.queryTest1")),
      "active" -> true,
      "unique" -> false
    )))

    val randomClassIndex = Await.result(randomClassIndexF, 5 second)(RefField).get
    val testIndex = Await.result(indexCreateF, 5 second)(RefField).get

    val randomText1 = Random.alphanumeric.take(8).mkString
    val randomText2 = Random.alphanumeric.take(8).mkString
    val randomText3 = Random.alphanumeric.take(8).mkString

    val createFuture = client.query(Create(classRef, Obj("data" -> Obj("queryTest1" -> randomText1))))
    val createFuture2 = client.query(Create(classRef, Obj("data" -> Obj("queryTest1" -> randomText2))))
    val createFuture3 = client.query(Create(classRef, Obj("data" -> Obj("queryTest1" -> randomText3))))

    val create1 = Await.result(createFuture, 1 second)
    val create2 = Await.result(createFuture2, 1 second)
    val create3 = Await.result(createFuture3, 1 second)

    val queryMatchF = client.query(Paginate(Match(testIndex, randomText1)))
    val queryMatchR = Await.result(queryMatchF, 1 second)

    queryMatchR(PageRefs).get shouldBe Seq(create1(RefField).get)

    val queryF = client.query(Paginate(Match(randomClassIndex), size = 1))
    val resp = Await.result(queryF, 5 seconds)

    resp("data").as[ArrayV].get.elems.size shouldBe 1
    resp("after").isDefined should equal (true)
    resp("before").isDefined should equal (false)

    val query2F = client.query(Paginate(Match(randomClassIndex), After(resp("after")), size = 1))
    val resp2 = Await.result(query2F, 5 seconds)

    resp2("data").as[Seq[Value]].get.size shouldBe 1
    resp2("after").isDefined should equal (true)
    resp2("before").isDefined should equal (true)

    val countF = client.query(Count(Match(randomClassIndex)))
    val countR = Await.result(countF, 1 second).as[Long].get
    countR shouldBe 3
  }

  it should "handle a constraint violation" in {
    val randomClassName = Random.alphanumeric.take(8).mkString
    val randomClassF = client.query(Create(Ref("classes"), Obj("name" -> randomClassName)))
    val classRef = Await.result(randomClassF, 1 second)(RefField).get

    val uniqueIndexFuture = client.query(Create(Ref("indexes"), Obj(
      "name" -> (randomClassName+"_by_unique_test"),
      "source" -> classRef,
      "terms" -> Arr(Obj("path" -> "data.uniqueTest1")),
      "unique" -> true, "active" -> true)))

    Await.result(uniqueIndexFuture, 1 second)

    val randomText = Random.alphanumeric.take(8).mkString
    val createFuture = client.query(Create(classRef, Obj("data" -> Obj("uniqueTest1" -> randomText))))

    Await.result(createFuture, 1 second)

    val createFuture2 = client.query(Create(classRef, Obj("data" -> Obj("uniqueTest1" -> randomText))))

    val exception = intercept[BadRequestException] {
      Await.result(createFuture2, 1 second)
    }

    exception.errors(0).code shouldBe "validation failed"
    exception.errors(0).failures.find(_.field == Seq("data", "uniqueTest1")).get.code shouldBe "duplicate value"
  }

  it should "test types" in {
    val setF = client.query(Match(Ref("indexes/spells_by_element"), "arcane"))
    val set = Await.result(setF, 1 second).as[SetRef].get
    set.parameters("match").as[Ref].get shouldBe Ref("indexes/spells_by_element")
    set.parameters("terms").as[String].get shouldBe "arcane"
  }

  it should "test basic forms" in {
    val letF = client.query(Let { val x = 1; val y = 2; x })
    val letR = Await.result(letF, 1 second)
    letR.as[Long].get shouldBe 1

    val ifF = client.query(If(true, "was true", "was false"))
    val ifR = Await.result(ifF, 1 second)
    ifR.as[String].get shouldBe "was true"

    val randomNum = Math.abs(Math.floorMod(Random.nextLong(), 250000L)) + 250000L
    val randomRef = Ref("classes/spells/" + randomNum)
    val doF = client.query(Do(
      Create(randomRef, Obj("data" -> Obj("name" -> "Magic Missile"))),
      Get(randomRef)
    ))
    val doR = Await.result(doF, 1 second)
    doR(RefField).get shouldBe randomRef

    val objectF = client.query(Obj("name" -> "Hen Wen", "age" -> 123))
    val objectR = Await.result(objectF, 1 second)
    objectR("name").as[String].get shouldBe "Hen Wen"
    objectR("age").as[Long].get shouldBe 123

  }

  it should "test collections" in {
    val mapF = client.query(Map(Lambda(munchings => Add(munchings, 1L)), Arr(1L, 2L, 3L)))
    val mapR = Await.result(mapF, 1 second)
    mapR.as[Seq[Long]].get shouldBe Seq(2, 3, 4)

    val foreachF = client.query(Foreach(Lambda(spell => Create(Ref("classes/spells"), Obj("data" -> Obj("name" -> spell)))), Arr("Fireball Level 1", "Fireball Level 2")))
    val foreachR = Await.result(foreachF, 1 second)
    foreachR.as[Seq[String]].get shouldBe Seq("Fireball Level 1", "Fireball Level 2")
  }

  it should "test resource modification" in {
    val createF = client.query(Create(Ref("classes/spells"), Obj("data" -> Obj("name" -> "Magic Missile", "element" -> "arcane", "cost" -> 10L))))
    val createR = Await.result(createF, 1 second)
    createR(RefField).get.value should startWith("classes/spells")
    createR("data", "name").as[String].get shouldBe "Magic Missile"
    createR("data", "element").as[String].get shouldBe "arcane"
    createR("data", "cost").as[Long].get shouldBe 10L

    val updateF = client.query(Update(createR(RefField), Obj("data" -> Obj("name" -> "Faerie Fire", "cost" -> NullV))))
    val updateR = Await.result(updateF, 1 second)
    updateR(RefField).get shouldBe createR(RefField).get
    updateR("data", "name").as[String].get shouldBe "Faerie Fire"
    updateR("data", "element").as[String].get shouldBe "arcane"
    updateR("data", "cost").isDefined should equal (false)

    val replaceF = client.query(Replace(createR("ref"), Obj("data" -> Obj("name" -> "Volcano", "element" -> Arr("fire", "earth"), "cost" -> 10L))))
    val replaceR = Await.result(replaceF, 1 second)
    replaceR("ref").get shouldBe createR("ref").get
    replaceR("data", "name").as[String].get shouldBe "Volcano"
    replaceR("data", "element").as[Seq[String]].get shouldBe Seq("fire", "earth")
    replaceR("data", "cost").as[Long].get shouldBe 10L

    val insertF = client.query(Insert(createR("ref"), 1L, Action.Create, Obj("data" -> Obj("cooldown" -> 5L))))
    val insertR = Await.result(insertF, 1 second)
    insertR("ref").get shouldBe createR("ref").get
    insertR("data").as[ObjectV].get should equal (ObjectV("cooldown" -> LongV(5)))

    val removeF = client.query(Remove(createR("ref"), 2L, Action.Delete))
    val removeR = Await.result(removeF, 1 second)
    removeR shouldBe NullV

    val deleteF = client.query(Delete(createR("ref")))
    Await.result(deleteF, 1 second)
    val getF = client.query(Get(createR("ref")))
    intercept[NotFoundException] {
      Await.result(getF, 1 second)
    }
  }

  it should "test sets" in {
    val create1F = client.query(Create(Ref("classes/spells"),
      Obj("data" -> Obj("name" -> "Magic Missile", "element" -> "arcane", "cost" -> 10L))))
    val create2F = client.query(Create(Ref("classes/spells"),
      Obj("data" -> Obj("name" -> "Fireball", "element" -> "fire", "cost" -> 10L))))
    val create3F = client.query(Create(Ref("classes/spells"),
      Obj("data" -> Obj("name" -> "Faerie Fire", "element" -> Arr("arcane", "nature"), "cost" -> 10L))))
    val create4F = client.query(Create(Ref("classes/spells"),
      Obj("data" -> Obj("name" -> "Summon Animal Companion", "element" -> "nature", "cost" -> 10L))))

    val create1R = Await.result(create1F, 1 second)
    val create2R = Await.result(create2F, 1 second)
    val create3R = Await.result(create3F, 1 second)
    val create4R = Await.result(create4F, 1 second)

    val matchF = client.query(Paginate(Match(Ref("indexes/spells_by_element"), "arcane")))
    val matchR = Await.result(matchF, 1 second)
    matchR("data").as[Seq[Ref]].get should contain (create1R("ref").get)

    val matchEventsF = client.query(Paginate(Match(Ref("indexes/spells_by_element"), "arcane"), events = true))
    val matchEventsR = Await.result(matchEventsF, 1 second)
    matchEventsR(PageEvents).get map { _.ref } should contain (create1R("ref").as[Ref].get)

    val unionF = client.query(Paginate(Union(
      Match(Ref("indexes/spells_by_element"), "arcane"),
      Match(Ref("indexes/spells_by_element"), "fire"))))
    val unionR = Await.result(unionF, 1 second)
    unionR(PageRefs).get should (contain (create1R(RefField).get) and contain (create2R(RefField).get))

    val unionEventsF = client.query(Paginate(Union(
      Match(Ref("indexes/spells_by_element"), "arcane"),
      Match(Ref("indexes/spells_by_element"), "fire")), events = true))
    val unionEventsR = Await.result(unionEventsF, 1 second)

    unionEventsR(PageEvents).get collect { case e if e.action == "create" => e.ref } should (
      contain (create1R(RefField).get) and contain (create2R(RefField).get))

    val intersectionF = client.query(Paginate(Intersection(
      Match(Ref("indexes/spells_by_element"), "arcane"),
      Match(Ref("indexes/spells_by_element"), "nature"))))
    val intersectionR = Await.result(intersectionF, 1 second)
    intersectionR(PageRefs).get should contain (create3R(RefField).get)

    val differenceF = client.query(Paginate(Difference(
      Match(Ref("indexes/spells_by_element"), "nature"),
      Match(Ref("indexes/spells_by_element"), "arcane"))))

    val differenceR = Await.result(differenceF, 1 second)
    differenceR(PageRefs).get should contain (create4R(RefField).get)
  }

  it should "test miscellaneous functions" in {
    val equalsF = client.query(Equals("fire", "fire"))
    val equalsR = Await.result(equalsF, 1 second).as[Boolean].get
    equalsR shouldBe true

    val concatF = client.query(Concat(Arr("Magic", "Missile")))
    val concatR = Await.result(concatF, 1 second).as[String].get
    concatR shouldBe "MagicMissile"

    val concat2F = client.query(Concat(Arr("Magic", "Missile"), " "))
    val concat2R = Await.result(concat2F, 1 second).as[String].get
    concat2R shouldBe "Magic Missile"

    val containsF = client.query(Contains("favorites" / "foods", Obj("favorites" -> Obj("foods" -> Arr("crunchings", "munchings")))))
    val containsR = Await.result(containsF, 1 second).as[Boolean].get
    containsR shouldBe true

    val selectF = client.query(Select("favorites" / "foods" / 1, Obj("favorites" -> Obj("foods" -> Arr("crunchings", "munchings", "lunchings")))))
    val selectR = Await.result(selectF, 1 second).as[String].get
    selectR shouldBe "munchings"

    val addF = client.query(Add(100L, 10L))
    val addR = Await.result(addF, 1 second).as[Long].get
    addR shouldBe 110L

    val multiplyF = client.query(Multiply(100L, 10L))
    val multiplyR = Await.result(multiplyF, 1 second).as[Long].get
    multiplyR shouldBe 1000L

    val subtractF = client.query(Subtract(100L, 10L))
    val subtractR = Await.result(subtractF, 1 second).as[Long].get
    subtractR shouldBe 90L

    val divideF = client.query(Divide(100L, 10L))
    val divideR = Await.result(divideF, 1 second).as[Long].get
    divideR shouldBe 10L

    val moduloF = client.query(Modulo(101L, 10L))
    val moduloR = Await.result(moduloF, 1 second).as[Long].get
    moduloR shouldBe 1L

    val andF = client.query(And(true, false))
    val andR = Await.result(andF, 1 second).as[Boolean].get
    andR shouldBe false

    val orF = client.query(Or(true, false))
    val orR = Await.result(orF, 1 second).as[Boolean].get
    orR shouldBe true

    val notF = client.query(Not(false))
    val notR = Await.result(notF, 1 second).as[Boolean].get
    notR shouldBe true
  }

  it should "test date and time functions" in {
    val timeF = client.query(Time("1970-01-01T00:00:00-04:00"))
    val timeR = Await.result(timeF, 1 second)
    timeR.as[Timestamp].get.instant shouldBe Instant.EPOCH.plus(4, ChronoUnit.HOURS)
    timeR.as[Instant].get shouldBe Instant.EPOCH.plus(4, ChronoUnit.HOURS)

    val epochF = client.query(Epoch(30, "second"))
    val epochR = Await.result(epochF, 1 second)
    epochR.as[Timestamp].get.instant shouldBe Instant.EPOCH.plus(30, ChronoUnit.SECONDS)
    epochR.as[Instant].get shouldBe Instant.EPOCH.plus(30, ChronoUnit.SECONDS)

    val dateF = client.query(query.Date("1970-01-02"))
    val dateR = Await.result(dateF, 1 second)
    dateR.as[Date].get.localDate shouldBe LocalDate.ofEpochDay(1)
    dateR.as[LocalDate].get shouldBe LocalDate.ofEpochDay(1)
  }

  it should "test authentication functions" in {
    val createF = client.query(Create(Ref("classes/spells"), Obj("credentials" -> Obj("password" -> "abcdefg"))))
    val createR = Await.result(createF, 1 second)

    val loginF = client.query(Login(createR("ref").as[Ref], Obj("password" -> "abcdefg")))
    val secret = Await.result(loginF, 1 second)("secret").as[String].get

    val sessionClient = FaunaClient(config("root_url"), secret = secret)

    val logoutF = sessionClient.query(Logout(false))
    val logoutR = Await.result(logoutF, 1 second)
    logoutR.as[Boolean].get shouldBe true

    val identifyF = client.query(Identify(createR("ref").as[Ref], "abcdefg"))
    val identifyR = Await.result(identifyF, 1 second)
    identifyR.as[Boolean].get shouldBe true

    sessionClient.close()
  }
}
