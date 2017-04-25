package faunadb

import faunadb.errors.{ BadRequestException, NotFoundException, PermissionDeniedException, UnauthorizedException }
import faunadb.query.TimeUnit._
import faunadb.query._
import faunadb.values._
import faunadb.values.time._
import org.joda.time
import org.joda.time.DateTimeZone.UTC
import org.joda.time.{ Instant, LocalDate }
import org.scalatest.{ BeforeAndAfterAll, FlatSpec, Matchers }
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{ Await, Future }
import scala.util.Random

class ClientSpec extends FlatSpec with Matchers with BeforeAndAfterAll {
  val config = {
    val rootKey = Option(System.getenv("FAUNA_ROOT_KEY")) getOrElse {
      throw new RuntimeException("FAUNA_ROOT_KEY must defined to run tests")
    }
    val domain = Option(System.getenv("FAUNA_DOMAIN")) getOrElse { "db.fauna.com" }
    val scheme = Option(System.getenv("FAUNA_SCHEME")) getOrElse { "https" }
    val port = Option(System.getenv("FAUNA_PORT")) getOrElse { "443" }

    collection.Map("root_token" -> rootKey, "root_url" -> s"${scheme}://${domain}:${port}")
  }

  val rootClient = FaunaClient(endpoint = config("root_url"), secret = config("root_token"))

  val testDbName = "faunadb-scala-test"
  var client: FaunaClient = null

  // Helper fields

  val RefField = Field("ref").to[RefV]
  val TsField = Field("ts").to[Long]
  val ClassField = Field("class").to[RefV]
  val SecretField = Field("secret").to[String]

  // Page helpers
  case class Ev(ref: RefV, ts: Long, action: String)

  val EventField = Field.zip(
    Field("resource").to[RefV],
    Field("ts").to[Long],
    Field("action").to[String]
  ) map { case (r, ts, a) => Ev(r, ts, a) }

  val PageEvents = Field("data").collect(EventField)
  val PageRefs = Field("data").to[Seq[RefV]]

  def await[T](f: Future[T]) = Await.result(f, 5.second)
  def ready[T](f: Future[T]) = Await.ready(f, 5.second)

  def dropDB(): Unit =
    ready(rootClient.query(Delete(Database(testDbName))))

  // tests

  override protected def beforeAll(): Unit = {
    dropDB()

    val db = await(rootClient.query(CreateDatabase(Obj("name" -> testDbName))))
    val dbRef = db(RefField).get
    val key = await(rootClient.query(CreateKey(Obj("database" -> dbRef, "role" -> "server"))))

    client = FaunaClient(endpoint = config("root_url"), secret = key(SecretField).get)

    await(client.query(CreateClass(Obj("name" -> "spells"))))

    await(client.query(CreateIndex(Obj(
      "name" -> "spells_by_element",
      "source" -> Class("spells"),
      "terms" -> Arr(Obj("path" -> "data.element")),
      "active" -> true))))
  }

  override protected def afterAll(): Unit = {
    dropDB()
    client.close()
    rootClient.close()
  }

  "Fauna Client" should "should not find an instance" in {
    a[NotFoundException] should be thrownBy (await(client.query(Get(Ref("classes/spells/1234")))))
  }

  it should "echo values" in {
    await(client.query(ObjectV("foo" -> StringV("bar")))) should equal (ObjectV("foo" -> StringV("bar")))
    await(client.query("qux")) should equal (StringV("qux"))
  }

  it should "fail with permission denied" in {
    val key = await(rootClient.query(CreateKey(Obj("database" -> Database(testDbName), "role" -> "client"))))
    val client = FaunaClient(endpoint = config("root_url"), secret = key(SecretField).get)

    an[PermissionDeniedException] should be thrownBy (await(client.query(Paginate(Ref("databases")))))
  }

  it should "fail with unauthorized" in {
    val badClient = FaunaClient(endpoint = config("root_url"), secret = "notavalidsecret")
    an[UnauthorizedException] should be thrownBy (await(badClient.query(Get(Ref("classes/spells/12345")))))
  }

  it should "create a new instance" in {
    val inst = await(client.query(
      Create(Class("spells"),
        Obj("data" -> Obj("testField" -> "testValue")))))

    inst(RefField).get.value should startWith ("classes/spells/")
    inst(ClassField).get should equal (RefV("classes/spells"))
    inst("data", "testField").to[String].get should equal ("testValue")

    await(client.query(Exists(inst(RefField)))) should equal (TrueV)

    val inst2 = await(client.query(Create(Class("spells"),
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
    testData("array", 0).to[Long].get shouldBe 1
    testData("array", 1).to[String].get shouldBe "2"
    testData("array", 2).to[Double].get shouldBe 3.4
    testData("string").to[String].get shouldBe "sup"
    testData( "num").to[Long].get shouldBe 1234
  }

  it should "issue a batched query" in {
    val randomText1 = Random.alphanumeric.take(8).mkString
    val randomText2 = Random.alphanumeric.take(8).mkString
    val classRef = Class("spells")
    val expr1 = Create(classRef, Obj("data" -> Obj("queryTest1" -> randomText1)))
    val expr2 = Create(classRef, Obj("data" -> Obj("queryTest1" -> randomText2)))

    val results = await(client.query(Seq(expr1, expr2)))

    results.length shouldBe 2
    results(0)("data", "queryTest1").to[String].get shouldBe randomText1
    results(1)("data", "queryTest1").to[String].get shouldBe randomText2
  }

  it should "get at timestamp" in {
    val randomClassName = Random.alphanumeric.take(8).mkString
    val randomClass = await(client.query(CreateClass(Obj("name" -> randomClassName))))

    val data = await(client.query(Create(randomClass(RefField).get, Obj("data" -> Obj("x" -> 1)))))
    val dataRef = data(RefField).get

    val ts1 = data(TsField).get
    val ts2 = await(client.query(Update(dataRef, Obj("data" -> Obj("x" -> 2)))))(TsField).get
    val ts3 = await(client.query(Update(dataRef, Obj("data" -> Obj("x" -> 3)))))(TsField).get

    val xField = Field("data", "x").to[Long]

    await(client.query(At(ts1, Get(dataRef))))(xField).get should equal(1)
    await(client.query(At(ts2, Get(dataRef))))(xField).get should equal(2)
    await(client.query(At(ts3, Get(dataRef))))(xField).get should equal(3)
  }

  it should "issue a paginated query" in {
    val randomClassName = Random.alphanumeric.take(8).mkString
    val randomClassF = client.query(CreateClass(Obj("name" -> randomClassName)))
    val classRef = await(randomClassF)(RefField).get

    val randomClassIndexF = client.query(CreateIndex(Obj(
      "name" -> (randomClassName + "_class_index"),
      "source" -> classRef,
      "active" -> true,
      "unique" -> false
    )))

    val indexCreateF = client.query(CreateIndex(Obj(
      "name" -> (randomClassName + "_test_index"),
      "source" -> classRef,
      "terms" -> Arr(Obj("path" -> "data.queryTest1")),
      "active" -> true,
      "unique" -> false
    )))

    val randomClassIndex = await(randomClassIndexF)(RefField).get
    val testIndex = await(indexCreateF)(RefField).get

    val randomText1 = Random.alphanumeric.take(8).mkString
    val randomText2 = Random.alphanumeric.take(8).mkString
    val randomText3 = Random.alphanumeric.take(8).mkString

    val createFuture = client.query(Create(classRef, Obj("data" -> Obj("queryTest1" -> randomText1))))
    val createFuture2 = client.query(Create(classRef, Obj("data" -> Obj("queryTest1" -> randomText2))))
    val createFuture3 = client.query(Create(classRef, Obj("data" -> Obj("queryTest1" -> randomText3))))

    val create1 = await(createFuture)
    val create2 = await(createFuture2)
    val create3 = await(createFuture3)

    val queryMatchF = client.query(Paginate(Match(testIndex, randomText1)))
    val queryMatchR = await(queryMatchF)

    queryMatchR(PageRefs).get shouldBe Seq(create1(RefField).get)

    val queryF = client.query(Paginate(Match(randomClassIndex), size = 1))
    val resp = await(queryF)

    resp("data").to[ArrayV].get.elems.size shouldBe 1
    resp("after").isDefined should equal (true)
    resp("before").isDefined should equal (false)

    val query2F = client.query(Paginate(Match(randomClassIndex), After(resp("after")), size = 1))
    val resp2 = await(query2F)

    resp2("data").to[Seq[Value]].get.size shouldBe 1
    resp2("after").isDefined should equal (true)
    resp2("before").isDefined should equal (true)
  }

  it should "handle a constraint violation" in {
    val randomClassName = Random.alphanumeric.take(8).mkString
    val randomClassF = client.query(CreateClass(Obj("name" -> randomClassName)))
    val classRef = await(randomClassF)(RefField).get

    val uniqueIndexFuture = client.query(CreateIndex(Obj(
      "name" -> (randomClassName+"_by_unique_test"),
      "source" -> classRef,
      "terms" -> Arr(Obj("path" -> "data.uniqueTest1")),
      "unique" -> true, "active" -> true)))

    await(uniqueIndexFuture)

    val randomText = Random.alphanumeric.take(8).mkString
    val createFuture = client.query(Create(classRef, Obj("data" -> Obj("uniqueTest1" -> randomText))))

    await(createFuture)

    val createFuture2 = client.query(Create(classRef, Obj("data" -> Obj("uniqueTest1" -> randomText))))

    val exception = intercept[BadRequestException] {
      await(createFuture2)
    }

    exception.errors(0).code shouldBe "instance not unique"
  }

  it should "test types" in {
    val setF = client.query(Match(Index("spells_by_element"), "arcane"))
    val set = await(setF).to[SetRefV].get
    set.parameters("match").to[RefV].get shouldBe RefV("indexes/spells_by_element")
    set.parameters("terms").to[String].get shouldBe "arcane"

    await(client.query(Array[Byte](0x1, 0x2, 0x3, 0x4))) should equal (BytesV(0x1, 0x2, 0x3, 0x4))
  }

  it should "test basic forms" in {
    val letF = client.query(Let { val x = 1; val y = 2; x })
    val letR = await(letF)
    letR.to[Long].get shouldBe 1

    val ifF = client.query(If(true, "was true", "was false"))
    val ifR = await(ifF)
    ifR.to[String].get shouldBe "was true"

    val randomNum = Math.abs(Random.nextLong() % 250000L) + 250000L
    val randomRef = "classes/spells/" + randomNum
    val doF = client.query(Do(
      Create(Ref(randomRef), Obj("data" -> Obj("name" -> "Magic Missile"))),
      Get(Ref(randomRef))
    ))
    val doR = await(doF)
    doR(RefField).get shouldBe RefV(randomRef)

    val objectF = client.query(Obj("name" -> "Hen Wen", "age" -> 123))
    val objectR = await(objectF)
    objectR("name").to[String].get shouldBe "Hen Wen"
    objectR("age").to[Long].get shouldBe 123

  }

  it should "test collections" in {
    val mapF = client.query(Map(Arr(1L, 2L, 3L), Lambda(munchings => Add(munchings, 1L))))
    val mapR = await(mapF)
    mapR.to[Seq[Long]].get shouldBe Seq(2, 3, 4)

    val foreachF = client.query(Foreach(Arr("Fireball Level 1", "Fireball Level 2"), Lambda(spell => Create(Class("spells"), Obj("data" -> Obj("name" -> spell))))))
    val foreachR = await(foreachF)
    foreachR.to[Seq[String]].get shouldBe Seq("Fireball Level 1", "Fireball Level 2")
  }

  it should "test resource modification" in {
    val createF = client.query(Create(Class("spells"), Obj("data" -> Obj("name" -> "Magic Missile", "element" -> "arcane", "cost" -> 10L))))
    val createR = await(createF)
    createR(RefField).get.value should startWith("classes/spells")
    createR("data", "name").to[String].get shouldBe "Magic Missile"
    createR("data", "element").to[String].get shouldBe "arcane"
    createR("data", "cost").to[Long].get shouldBe 10L

    val updateF = client.query(Update(createR(RefField), Obj("data" -> Obj("name" -> "Faerie Fire", "cost" -> NullV))))
    val updateR = await(updateF)
    updateR(RefField).get shouldBe createR(RefField).get
    updateR("data", "name").to[String].get shouldBe "Faerie Fire"
    updateR("data", "element").to[String].get shouldBe "arcane"
    updateR("data", "cost").isDefined should equal (false)

    val replaceF = client.query(Replace(createR("ref"), Obj("data" -> Obj("name" -> "Volcano", "element" -> Arr("fire", "earth"), "cost" -> 10L))))
    val replaceR = await(replaceF)
    replaceR("ref").get shouldBe createR("ref").get
    replaceR("data", "name").to[String].get shouldBe "Volcano"
    replaceR("data", "element").to[Seq[String]].get shouldBe Seq("fire", "earth")
    replaceR("data", "cost").to[Long].get shouldBe 10L

    val insertF = client.query(Insert(createR("ref"), 1L, Action.Create, Obj("data" -> Obj("cooldown" -> 5L))))
    val insertR = await(insertF)
    insertR("resource").get shouldBe createR("ref").get

    val removeF = client.query(Remove(createR("ref"), 2L, Action.Delete))
    val removeR = await(removeF)
    removeR shouldBe NullV

    val deleteF = client.query(Delete(createR("ref")))
    await(deleteF)
    val getF = client.query(Get(createR("ref")))
    intercept[NotFoundException] {
      await(getF)
    }
  }

  it should "test sets" in {
    val create1F = client.query(Create(Class("spells"),
      Obj("data" -> Obj("name" -> "Magic Missile", "element" -> "arcane", "cost" -> 10L))))
    val create2F = client.query(Create(Class("spells"),
      Obj("data" -> Obj("name" -> "Fireball", "element" -> "fire", "cost" -> 10L))))
    val create3F = client.query(Create(Class("spells"),
      Obj("data" -> Obj("name" -> "Faerie Fire", "element" -> Arr("arcane", "nature"), "cost" -> 10L))))
    val create4F = client.query(Create(Class("spells"),
      Obj("data" -> Obj("name" -> "Summon Animal Companion", "element" -> "nature", "cost" -> 10L))))

    val create1R = await(create1F)
    val create2R = await(create2F)
    val create3R = await(create3F)
    val create4R = await(create4F)

    val matchF = client.query(Paginate(Match(Index("spells_by_element"), "arcane")))
    val matchR = await(matchF)
    matchR("data").to[Seq[RefV]].get should contain (create1R("ref").get)

    val matchEventsF = client.query(Paginate(Match(Index("spells_by_element"), "arcane"), events = true))
    val matchEventsR = await(matchEventsF)
    matchEventsR(PageEvents).get map { _.ref } should contain (create1R("ref").to[RefV].get)

    val unionF = client.query(Paginate(Union(
      Match(Index("spells_by_element"), "arcane"),
      Match(Index("spells_by_element"), "fire"))))
    val unionR = await(unionF)
    unionR(PageRefs).get should (contain (create1R(RefField).get) and contain (create2R(RefField).get))

    val unionEventsF = client.query(Paginate(Union(
      Match(Index("spells_by_element"), "arcane"),
      Match(Index("spells_by_element"), "fire")), events = true))
    val unionEventsR = await(unionEventsF)

    unionEventsR(PageEvents).get collect { case e if e.action == "create" => e.ref } should (
      contain (create1R(RefField).get) and contain (create2R(RefField).get))

    val intersectionF = client.query(Paginate(Intersection(
      Match(Index("spells_by_element"), "arcane"),
      Match(Index("spells_by_element"), "nature"))))
    val intersectionR = await(intersectionF)
    intersectionR(PageRefs).get should contain (create3R(RefField).get)

    val differenceF = client.query(Paginate(Difference(
      Match(Index("spells_by_element"), "nature"),
      Match(Index("spells_by_element"), "arcane"))))

    val differenceR = await(differenceF)
    differenceR(PageRefs).get should contain (create4R(RefField).get)
  }

  it should "test miscellaneous functions" in {
    val nextIdF = client.query(NextId())
    val nextIdR = await(nextIdF).to[String].get
    nextIdR should not be null

    val equalsF = client.query(Equals("fire", "fire"))
    val equalsR = await(equalsF).to[Boolean].get
    equalsR shouldBe true

    val concatF = client.query(Concat(Arr("Magic", "Missile")))
    val concatR = await(concatF).to[String].get
    concatR shouldBe "MagicMissile"

    val concat2F = client.query(Concat(Arr("Magic", "Missile"), " "))
    val concat2R = await(concat2F).to[String].get
    concat2R shouldBe "Magic Missile"

    val containsF = client.query(Contains("favorites" / "foods", Obj("favorites" -> Obj("foods" -> Arr("crunchings", "munchings")))))
    val containsR = await(containsF).to[Boolean].get
    containsR shouldBe true

    await(client.query(Contains("field", Obj("field" -> "value")))) shouldBe TrueV
    await(client.query(Contains(1, Arr("value0", "value1", "value2")))) shouldBe TrueV

    val selectF = client.query(Select("favorites" / "foods" / 1, Obj("favorites" -> Obj("foods" -> Arr("crunchings", "munchings", "lunchings")))))
    val selectR = await(selectF).to[String].get
    selectR shouldBe "munchings"

    await(client.query(Select("field", Obj("field" -> "value")))) shouldBe StringV("value")
    await(client.query(Select("non-existent-field", Obj("field" -> "value"), "a default value"))) shouldBe StringV("a default value")

    await(client.query(Select(1, Arr("value0", "value1", "value2")))) shouldBe StringV("value1")
    await(client.query(Select(100, Arr("value0", "value1", "value2"), "a default value"))) shouldBe StringV("a default value")

    await(client.query(Contains("a" / "nested" / 0 / "path", Obj("a" -> Obj("nested" -> Arr(Obj("path" -> "value"))))))) shouldBe TrueV
    await(client.query(Select("a" / "nested" / 0 / "path", Obj("a" -> Obj("nested" -> Arr(Obj("path" -> "value"))))))) shouldBe StringV("value")

    val addF = client.query(Add(100L, 10L))
    val addR = await(addF).to[Long].get
    addR shouldBe 110L

    val multiplyF = client.query(Multiply(100L, 10L))
    val multiplyR = await(multiplyF).to[Long].get
    multiplyR shouldBe 1000L

    val subtractF = client.query(Subtract(100L, 10L))
    val subtractR = await(subtractF).to[Long].get
    subtractR shouldBe 90L

    val divideF = client.query(Divide(100L, 10L))
    val divideR = await(divideF).to[Long].get
    divideR shouldBe 10L

    val moduloF = client.query(Modulo(101L, 10L))
    val moduloR = await(moduloF).to[Long].get
    moduloR shouldBe 1L

    val andF = client.query(And(true, false))
    val andR = await(andF).to[Boolean].get
    andR shouldBe false

    val orF = client.query(Or(true, false))
    val orR = await(orF).to[Boolean].get
    orR shouldBe true

    val notF = client.query(Not(false))
    val notR = await(notF).to[Boolean].get
    notR shouldBe true
  }

  it should "test date and time functions" in {
    val timeF = client.query(Time("1970-01-01T00:00:00-04:00"))
    val timeR = await(timeF)
    timeR.to[TimeV].get.time.toInstant shouldBe new Instant(0).plus(time.Duration.standardHours(4))
    timeR.to[Instant].get shouldBe new Instant(0).plus(time.Duration.standardHours(4))

    val epochR = await(client.query(Arr(
      Epoch(30, Second),
      Epoch(10, Millisecond),
      Epoch(42, Nanosecond),
      Epoch(40, Microsecond)
    )))

    epochR.collect(Field.to[HighPrecisionTime]).get.sorted shouldBe Seq(
      HighPrecisionTime(new Instant(0), nanosToAdd = 42),
      HighPrecisionTime(new Instant(0), microsToAdd = 40),
      HighPrecisionTime(new Instant(10)),
      HighPrecisionTime(new Instant(30000))
    )

    epochR(0).to[TimeV].get.time.toInstant shouldBe new Instant(0).plus(time.Duration.standardSeconds(30))
    epochR(0).to[Instant].get shouldBe new Instant(0).plus(time.Duration.standardSeconds(30))

    val dateF = client.query(query.Date("1970-01-02"))
    val dateR = await(dateF)
    dateR.to[DateV].get.localDate shouldBe new LocalDate(0, UTC).plusDays(1)
    dateR.to[LocalDate].get shouldBe new LocalDate(0, UTC).plusDays(1)
  }

  it should "test authentication functions" in {
    val createF = client.query(Create(Class("spells"), Obj("credentials" -> Obj("password" -> "abcdefg"))))
    val createR = await(createF)

    val loginF = client.query(Login(createR("ref").to[RefV], Obj("password" -> "abcdefg")))
    val secret = await(loginF)("secret").to[String].get

    val loggedOut = client.sessionWith(secret)(_.query(Logout(false)))
    await(loggedOut).to[Boolean].get shouldBe true

    val identifyF = client.query(Identify(createR("ref").to[RefV], "abcdefg"))
    val identifyR = await(identifyF)
    identifyR.to[Boolean].get shouldBe true
  }

  it should "find key by secret" in {
    val key = await(rootClient.query(CreateKey(Obj("database" -> Database(testDbName), "role" -> "admin"))))

    await(rootClient.query(KeyFromSecret(key(SecretField).get))) should equal(await(rootClient.query(Get(key(RefField).get))))
  }

  it should "create a function" in {
    val query = Query((a, b) => Concat(Arr(a, b), "/"))

    await(client.query(CreateFunction(Obj("name" -> "a_function", "body" -> query))))

    await(client.query(Exists(Ref("functions/a_function")))).to[Boolean].get shouldBe true
  }

  it should "call a function" in  {
    val query = Query((a, b) => Concat(Arr(a, b), "/"))

    await(client.query(CreateFunction(Obj("name" -> "concat_with_slash", "body" -> query))))

    await(client.query(Call(Ref("functions/concat_with_slash"), "a", "b"))).to[String].get shouldBe "a/b"
  }

  case class Spell(name: String, element: Either[String, Seq[String]], cost: Option[Long])

  implicit val spellCodec: Codec[Spell] = Codec.caseClass[Spell]

  it should "encode/decode user classes" in {
    val masterSummon = Spell("Master Summon", Left("wind"), None)
    val magicMissile = Spell("Magic Missile", Left("arcane"), Some(10))
    val faerieFire = Spell("Faerie Fire", Right(Seq("arcane", "nature")), Some(10))

    val masterSummonCreated = await(client.query(Create(Class("spells"), Obj("data" -> masterSummon))))
    masterSummonCreated("data").to[Spell].get shouldBe masterSummon

    val spells = await(client.query(Map(Paginate(Match(Index("spells_by_element"), "arcane")), Lambda(x => Select("data", Get(x))))))("data").get
    spells.to[Set[Spell]].get shouldBe Set(magicMissile, faerieFire)
  }
}
