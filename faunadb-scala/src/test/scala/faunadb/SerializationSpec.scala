package faunadb

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import faunadb.query._
import faunadb.values._
import java.time.{ Duration, Instant, LocalDate }
import java.time.ZoneOffset.UTC
import java.time.temporal.ChronoUnit
import org.scalatest.{ FlatSpec, Matchers }

class SerializationSpec extends FlatSpec with Matchers {
  val json = new ObjectMapper()
  json.registerModule(new DefaultScalaModule)

  "Query AST serialization" should "serialize ref" in {
    val ref = RefV("ref", RefV("some", Native.Classes))
    json.writeValueAsString(ref) shouldBe "{\"@ref\":{\"id\":\"ref\",\"class\":{\"@ref\":{\"id\":\"some\",\"class\":{\"@ref\":{\"id\":\"classes\"}}}}}}"
  }

  it should "serialize literal values" in {
    json.writeValueAsString(BooleanV(true)) shouldBe "true"
    json.writeValueAsString(BooleanV(false)) shouldBe "false"
    json.writeValueAsString(StringV("test")) shouldBe "\"test\""
    json.writeValueAsString(LongV(1234)) shouldBe "1234"
    json.writeValueAsString(LongV(Long.MaxValue)) shouldBe Long.MaxValue.toString
    json.writeValueAsString(DoubleV(1.234)) shouldBe "1.234"
    json.writeValueAsString(NullV) shouldBe "null"
  }

  it should "serialize bytes" in {
    json.writeValueAsString(BytesV(0x1, 0x2, 0x3, 0x4)) shouldBe "{\"@bytes\":\"AQIDBA==\"}"

    json.writeValueAsString(BytesV(0xf8)) shouldBe "{\"@bytes\":\"-A==\"}"
    json.writeValueAsString(BytesV(0xf9)) shouldBe "{\"@bytes\":\"-Q==\"}"
    json.writeValueAsString(BytesV(0xfa)) shouldBe "{\"@bytes\":\"-g==\"}"
    json.writeValueAsString(BytesV(0xfb)) shouldBe "{\"@bytes\":\"-w==\"}"
    json.writeValueAsString(BytesV(0xfc)) shouldBe "{\"@bytes\":\"_A==\"}"
    json.writeValueAsString(BytesV(0xfd)) shouldBe "{\"@bytes\":\"_Q==\"}"
    json.writeValueAsString(BytesV(0xfe)) shouldBe "{\"@bytes\":\"_g==\"}"
    json.writeValueAsString(BytesV(0xff)) shouldBe "{\"@bytes\":\"_w==\"}"
  }

  it should "serialize complex values" in {
    json.writeValueAsString(Arr(1, "test")) shouldBe "[1,\"test\"]"
    json.writeValueAsString(Arr(Arr(Obj("test" -> "value"), 2323, true), "hi", Obj("test" -> "yo", "test2" -> NullV))) shouldBe "[[{\"object\":{\"test\":\"value\"}},2323,true],\"hi\",{\"object\":{\"test\":\"yo\",\"test2\":null}}]"
    json.writeValueAsString(Obj("test" -> 1, "test2" -> RefV("ref", RefV("some", Native.Classes)))) shouldBe "{\"object\":{\"test\":1,\"test2\":{\"@ref\":{\"id\":\"ref\",\"class\":{\"@ref\":{\"id\":\"some\",\"class\":{\"@ref\":{\"id\":\"classes\"}}}}}}}}"
  }

  it should "serialize basic forms" in {
    json.writeValueAsString(Abort("a message")) shouldBe "{\"abort\":\"a message\"}"

    val let = Let { val x = 1; val y = "2"; x }
    json.writeValueAsString(let) shouldBe "{\"let\":{\"x\":1,\"y\":\"2\"},\"in\":{\"var\":\"x\"}}"

    val let2 = Let { val x = 1; val y = "2"; { val z = "foo"; x } }
    json.writeValueAsString(let2) shouldBe "{\"let\":{\"x\":1,\"y\":\"2\"},\"in\":{\"var\":\"x\"}}"

    val let3 = { val x0 = Var("x"); Let { val x = 1; val y = "2"; x0 } }
    json.writeValueAsString(let3) shouldBe "{\"let\":{\"x\":1,\"y\":\"2\"},\"in\":{\"var\":\"x\"}}"

    val let4 = Let(Seq("x" -> 1, "y" -> "2"), Var("x"))
    json.writeValueAsString(let4) shouldBe "{\"let\":{\"x\":1,\"y\":\"2\"},\"in\":{\"var\":\"x\"}}"

    val let5 = Let { val x = 1; val _ = "2"; x }
    json.writeValueAsString(let5) shouldBe "{\"let\":{\"x\":1,\"_\":\"2\"},\"in\":{\"var\":\"x\"}}"

    val ifForm = If(true, "was true", "was false")
    json.writeValueAsString(ifForm) shouldBe "{\"if\":true,\"then\":\"was true\",\"else\":\"was false\"}"

    val doForm = Do(
      Create(RefV("ref1", RefV("some", Native.Classes)), Obj("data" -> Obj("name" -> "Hen Wen"))),
      Get(RefV("ref1", RefV("some", Native.Classes))))
    json.writeValueAsString(doForm) shouldBe "{\"do\":[{\"create\":{\"@ref\":{\"id\":\"ref1\",\"class\":{\"@ref\":{\"id\":\"some\",\"class\":{\"@ref\":{\"id\":\"classes\"}}}}}},\"params\":{\"object\":{\"data\":{\"object\":{\"name\":\"Hen Wen\"}}}}},{\"get\":{\"@ref\":{\"id\":\"ref1\",\"class\":{\"@ref\":{\"id\":\"some\",\"class\":{\"@ref\":{\"id\":\"classes\"}}}}}}}]}"

    val select = Select("favorites" / "foods" / 1, Obj("favorites" -> Obj("foods" -> Arr("crunchings", "munchings", "lunchings"))))
    json.writeValueAsString(select) shouldBe "{\"select\":[\"favorites\",\"foods\",1],\"from\":{\"object\":{\"favorites\":{\"object\":{\"foods\":[\"crunchings\",\"munchings\",\"lunchings\"]}}}}}"

    val selectAll = SelectAll("foo" / "bar", Arr(Obj("foo" -> "bar")))
    json.writeValueAsString(selectAll) shouldBe "{\"select_all\":[\"foo\",\"bar\"],\"from\":[{\"object\":{\"foo\":\"bar\"}}]}"

    val lambda1 = Lambda(a => a)
    json.writeValueAsString(lambda1) should equal ("""{"lambda":"a","expr":{"var":"a"}}""")

    val lambda2 = Lambda((a, b) => Arr(b, a))
    json.writeValueAsString(lambda2) should equal ("""{"lambda":["a","b"],"expr":[{"var":"b"},{"var":"a"}]}""")

    val lambda3 = Lambda((a, _, _) => a)
    json.writeValueAsString(lambda3) should equal ("""{"lambda":["a","_","_"],"expr":{"var":"a"}}""")

    val lambda4 = Lambda(Not(_))
    json.writeValueAsString(lambda4) should equal ("""{"lambda":"x$3","expr":{"not":{"var":"x$3"}}}""")

    json.writeValueAsString(At(1L, Get(Native.Classes))) should equal ("""{"at":1,"expr":{"get":{"@ref":{"id":"classes"}}}}""")
  }

  it should "serialize collections" in {
    val map = Map(Arr(1, 2, 3), Lambda(munchings => munchings))
    json.writeValueAsString(map) shouldBe "{\"map\":{\"lambda\":\"munchings\",\"expr\":{\"var\":\"munchings\"}},\"collection\":[1,2,3]}"

    val map2 = Map(Arr(1, 2, 3), Lambda("munchings", Var("munchings")))
    json.writeValueAsString(map2) shouldBe "{\"map\":{\"lambda\":\"munchings\",\"expr\":{\"var\":\"munchings\"}},\"collection\":[1,2,3]}"

    val foreach = Foreach(Arr(RefV("ref1", RefV("another", Native.Classes)), RefV("ref2", RefV("another", Native.Classes))), Lambda(creature => Create(RefV("some", Native.Classes), Obj("data" -> Obj("some" -> creature)))))
    json.writeValueAsString(foreach) shouldBe "{\"foreach\":{\"lambda\":\"creature\",\"expr\":{\"create\":{\"@ref\":{\"id\":\"some\",\"class\":{\"@ref\":{\"id\":\"classes\"}}}},\"params\":{\"object\":{\"data\":{\"object\":{\"some\":{\"var\":\"creature\"}}}}}}},\"collection\":[{\"@ref\":{\"id\":\"ref1\",\"class\":{\"@ref\":{\"id\":\"another\",\"class\":{\"@ref\":{\"id\":\"classes\"}}}}}},{\"@ref\":{\"id\":\"ref2\",\"class\":{\"@ref\":{\"id\":\"another\",\"class\":{\"@ref\":{\"id\":\"classes\"}}}}}}]}"

    val filter = Filter(Arr(1,2,3), Lambda(i => Equals(1, i)))
    json.writeValueAsString(filter) shouldBe "{\"filter\":{\"lambda\":\"i\",\"expr\":{\"equals\":[1,{\"var\":\"i\"}]}},\"collection\":[1,2,3]}"

    val take = Take(2, Arr(1,2,3))
    json.writeValueAsString(take) shouldBe "{\"take\":2,\"collection\":[1,2,3]}"

    val drop = Drop(2, Arr(1,2,3))
    json.writeValueAsString(drop) shouldBe "{\"drop\":2,\"collection\":[1,2,3]}"

    val prepend = Prepend(Arr(4,5,6), Arr(1,2,3))
    json.writeValueAsString(prepend) shouldBe "{\"prepend\":[4,5,6],\"collection\":[1,2,3]}"

    val append = Append(Arr(1,2,3), Arr(4,5,6))
    json.writeValueAsString(append) shouldBe "{\"append\":[1,2,3],\"collection\":[4,5,6]}"
  }

  it should "serialize resource retrievals" in {
    val ref = RefV("ref1", RefV("some", Native.Classes))
    val get = Get(ref)
    json.writeValueAsString(get) shouldBe "{\"get\":{\"@ref\":{\"id\":\"ref1\",\"class\":{\"@ref\":{\"id\":\"some\",\"class\":{\"@ref\":{\"id\":\"classes\"}}}}}}}"

    val paginate1 = Paginate(Union(Match(RefV("some_index", Native.Indexes), "term"), Match(RefV("some_index", Native.Indexes), "term2")))
    json.writeValueAsString(paginate1) shouldBe "{\"paginate\":{\"union\":[{\"match\":\"term\",\"index\":{\"@ref\":{\"id\":\"some_index\",\"class\":{\"@ref\":{\"id\":\"indexes\"}}}}},{\"match\":\"term2\",\"index\":{\"@ref\":{\"id\":\"some_index\",\"class\":{\"@ref\":{\"id\":\"indexes\"}}}}}]}}"

    val paginate2 = Paginate(Union(Match(RefV("some_index", Native.Indexes), "term"), Match(RefV("some_index", Native.Indexes), "term2")), sources = true)
    json.writeValueAsString(paginate2) shouldBe "{\"paginate\":{\"union\":[{\"match\":\"term\",\"index\":{\"@ref\":{\"id\":\"some_index\",\"class\":{\"@ref\":{\"id\":\"indexes\"}}}}},{\"match\":\"term2\",\"index\":{\"@ref\":{\"id\":\"some_index\",\"class\":{\"@ref\":{\"id\":\"indexes\"}}}}}]},\"sources\":true}"

    val paginate3 = Paginate(Union(Match(RefV("some_index", Native.Indexes), "term"), Match(RefV("some_index", Native.Indexes), "term2")), events = true)
    json.writeValueAsString(paginate3) shouldBe "{\"paginate\":{\"union\":[{\"match\":\"term\",\"index\":{\"@ref\":{\"id\":\"some_index\",\"class\":{\"@ref\":{\"id\":\"indexes\"}}}}},{\"match\":\"term2\",\"index\":{\"@ref\":{\"id\":\"some_index\",\"class\":{\"@ref\":{\"id\":\"indexes\"}}}}}]},\"events\":true}"

    val paginate4 = Paginate(Union(Match(RefV("some_index", Native.Indexes), "term"), Match(RefV("some_index", Native.Indexes), "term2")), Before(RefV("ref1", RefV("some", Native.Classes))), size = 4)
    json.writeValueAsString(paginate4) shouldBe "{\"paginate\":{\"union\":[{\"match\":\"term\",\"index\":{\"@ref\":{\"id\":\"some_index\",\"class\":{\"@ref\":{\"id\":\"indexes\"}}}}},{\"match\":\"term2\",\"index\":{\"@ref\":{\"id\":\"some_index\",\"class\":{\"@ref\":{\"id\":\"indexes\"}}}}}]},\"before\":{\"@ref\":{\"id\":\"ref1\",\"class\":{\"@ref\":{\"id\":\"some\",\"class\":{\"@ref\":{\"id\":\"classes\"}}}}}},\"size\":4}"
  }

  it should "serialize resource modifications" in {
    val create = Create(RefV("spells", Native.Classes),
      Obj("data" -> Obj(
        "name" -> "Mountainous Thunder",
        "element" -> "air",
        "cost" -> 15)))

    json.writeValueAsString(create) shouldBe "{\"create\":{\"@ref\":{\"id\":\"spells\",\"class\":{\"@ref\":{\"id\":\"classes\"}}}},\"params\":{\"object\":{\"data\":{\"object\":{\"name\":\"Mountainous Thunder\",\"element\":\"air\",\"cost\":15}}}}}"

    val update = Update(RefV("123456", RefV("spells", Native.Classes)),
      Obj("data" -> Obj(
        "name" -> "Mountain's Thunder",
        "cost" -> NullV)))

    json.writeValueAsString(update) shouldBe "{\"update\":{\"@ref\":{\"id\":\"123456\",\"class\":{\"@ref\":{\"id\":\"spells\",\"class\":{\"@ref\":{\"id\":\"classes\"}}}}}},\"params\":{\"object\":{\"data\":{\"object\":{\"name\":\"Mountain's Thunder\",\"cost\":null}}}}}"

    val replace = Replace(RefV("123456", RefV("spells", Native.Classes)),
      Obj("data" -> Obj(
        "name" -> "Mountain's Thunder",
        "element" -> Arr("air", "earth"),
        "cost" -> 10)))

    json.writeValueAsString(replace) shouldBe "{\"replace\":{\"@ref\":{\"id\":\"123456\",\"class\":{\"@ref\":{\"id\":\"spells\",\"class\":{\"@ref\":{\"id\":\"classes\"}}}}}},\"params\":{\"object\":{\"data\":{\"object\":{\"name\":\"Mountain's Thunder\",\"element\":[\"air\",\"earth\"],\"cost\":10}}}}}"

    val delete = Delete(RefV("123456", RefV("spells", Native.Classes)))
    json.writeValueAsString(delete) shouldBe "{\"delete\":{\"@ref\":{\"id\":\"123456\",\"class\":{\"@ref\":{\"id\":\"spells\",\"class\":{\"@ref\":{\"id\":\"classes\"}}}}}}}"

    val insert = Insert(RefV("123456", RefV("spells", Native.Classes)), 1L, Action.Create, Obj("data" -> Obj("name" -> "Mountain's Thunder", "cost" -> 10, "element" -> Arr("air", "earth"))))
    json.writeValueAsString(insert) shouldBe "{\"insert\":{\"@ref\":{\"id\":\"123456\",\"class\":{\"@ref\":{\"id\":\"spells\",\"class\":{\"@ref\":{\"id\":\"classes\"}}}}}},\"ts\":1,\"action\":\"create\",\"params\":{\"object\":{\"data\":{\"object\":{\"name\":\"Mountain's Thunder\",\"cost\":10,\"element\":[\"air\",\"earth\"]}}}}}"

    val remove = Remove(RefV("123456", RefV("spells", Native.Classes)), 1L, Action.Delete)
    json.writeValueAsString(remove) shouldBe "{\"remove\":{\"@ref\":{\"id\":\"123456\",\"class\":{\"@ref\":{\"id\":\"spells\",\"class\":{\"@ref\":{\"id\":\"classes\"}}}}}},\"ts\":1,\"action\":\"delete\"}"

    val createClass = CreateClass(Obj("name" -> "spells"))
    json.writeValueAsString(createClass) shouldBe "{\"create_class\":{\"object\":{\"name\":\"spells\"}}}"

    val createDatabase = CreateDatabase(Obj("name" -> "db-test"))
    json.writeValueAsString(createDatabase) shouldBe "{\"create_database\":{\"object\":{\"name\":\"db-test\"}}}"

    val createKey = CreateKey(Obj("database" -> Database("db-test"), "role" -> "server"))
    json.writeValueAsString(createKey) shouldBe "{\"create_key\":{\"object\":{\"database\":{\"database\":\"db-test\"},\"role\":\"server\"}}}"

    val createIndex = CreateIndex(Obj("name" -> "all_spells", "source" -> Class("spells")))
    json.writeValueAsString(createIndex) shouldBe "{\"create_index\":{\"object\":{\"name\":\"all_spells\",\"source\":{\"class\":\"spells\"}}}}"
  }

  it should "serialize sets" in {
    val singleton = Singleton(Ref("classes/widget/1"))
    json.writeValueAsString(singleton) shouldBe "{\"singleton\":{\"@ref\":\"classes/widget/1\"}}"

    val events = Events(Ref("classes/widget/1"))
    json.writeValueAsString(events) shouldBe "{\"events\":{\"@ref\":\"classes/widget/1\"}}"

    val matchSet = Match(RefV("spells_by_elements", Native.Indexes), "fire")
    json.writeValueAsString(matchSet) shouldBe "{\"match\":\"fire\",\"index\":{\"@ref\":{\"id\":\"spells_by_elements\",\"class\":{\"@ref\":{\"id\":\"indexes\"}}}}}"

    val union = Union(Match(RefV("spells_by_element", Native.Indexes), "fire"), Match(RefV("spells_by_element", Native.Indexes), "water"))
    json.writeValueAsString(union) shouldBe "{\"union\":[{\"match\":\"fire\",\"index\":{\"@ref\":{\"id\":\"spells_by_element\",\"class\":{\"@ref\":{\"id\":\"indexes\"}}}}},{\"match\":\"water\",\"index\":{\"@ref\":{\"id\":\"spells_by_element\",\"class\":{\"@ref\":{\"id\":\"indexes\"}}}}}]}"

    val intersection = Intersection(Match(RefV("spells_by_element", Native.Indexes), "fire"), Match(RefV("spells_by_element", Native.Indexes), "water"))
    json.writeValueAsString(intersection) shouldBe "{\"intersection\":[{\"match\":\"fire\",\"index\":{\"@ref\":{\"id\":\"spells_by_element\",\"class\":{\"@ref\":{\"id\":\"indexes\"}}}}},{\"match\":\"water\",\"index\":{\"@ref\":{\"id\":\"spells_by_element\",\"class\":{\"@ref\":{\"id\":\"indexes\"}}}}}]}"

    val difference = Difference(Match(RefV("spells_by_element", Native.Indexes), "fire"), Match(RefV("spells_by_element", Native.Indexes), "water"))
    json.writeValueAsString(difference) shouldBe "{\"difference\":[{\"match\":\"fire\",\"index\":{\"@ref\":{\"id\":\"spells_by_element\",\"class\":{\"@ref\":{\"id\":\"indexes\"}}}}},{\"match\":\"water\",\"index\":{\"@ref\":{\"id\":\"spells_by_element\",\"class\":{\"@ref\":{\"id\":\"indexes\"}}}}}]}"

    val join = Join(Match(RefV("spells_by_element", Native.Indexes), "fire"), Lambda(spell => Get(spell)))
    json.writeValueAsString(join) shouldBe "{\"join\":{\"match\":\"fire\",\"index\":{\"@ref\":{\"id\":\"spells_by_element\",\"class\":{\"@ref\":{\"id\":\"indexes\"}}}}},\"with\":{\"lambda\":\"spell\",\"expr\":{\"get\":{\"var\":\"spell\"}}}}"
  }

  it should "serialize date and ts" in {
    val ts = TimeV(Instant.ofEpochMilli(0).plus(5, ChronoUnit.MINUTES))
    json.writeValueAsString(ts) shouldBe "{\"@ts\":\"1970-01-01T00:05:00Z\"}"

    val withMillis = TimeV(Instant.ofEpochMilli(1))
    json.writeValueAsString(withMillis) shouldBe "{\"@ts\":\"1970-01-01T00:00:00.001Z\"}"

    val withMicros = TimeV(Instant.ofEpochMilli(1).plus(1001, ChronoUnit.MICROS))
    json.writeValueAsString(withMicros) shouldBe "{\"@ts\":\"1970-01-01T00:00:00.002001Z\"}"

    val withNanos = TimeV(Instant.ofEpochMilli(1).plus(1001, ChronoUnit.NANOS))
    json.writeValueAsString(withNanos) shouldBe "{\"@ts\":\"1970-01-01T00:00:00.001001001Z\"}"

    val microsOverflow = TimeV(Instant.ofEpochMilli(1000).plus(1777777777, ChronoUnit.MICROS))
    json.writeValueAsString(microsOverflow) shouldBe "{\"@ts\":\"1970-01-01T00:29:38.777777Z\"}"

    val nanosOverflow = TimeV(Instant.ofEpochMilli(1000).plus(1999999999, ChronoUnit.NANOS))
    json.writeValueAsString(nanosOverflow) shouldBe "{\"@ts\":\"1970-01-01T00:00:02.999999999Z\"}"

    val microAndNanosOverflow = TimeV(Instant.ofEpochMilli(1000).plus(1000000, ChronoUnit.MICROS).plus(1000000000, ChronoUnit.NANOS))
    json.writeValueAsString(microAndNanosOverflow) shouldBe "{\"@ts\":\"1970-01-01T00:00:03Z\"}"

    val date = DateV(LocalDate.ofEpochDay(0).plusDays(2))
    json.writeValueAsString(date) shouldBe "{\"@date\":\"1970-01-03\"}"
  }

  it should "serialize authentication functions" in {
    val login = Login(RefV("104979509695139637", RefV("characters", Native.Classes)), Obj("password" -> "abracadabra"))
    json.writeValueAsString(login) shouldBe "{\"login\":{\"@ref\":{\"id\":\"104979509695139637\",\"class\":{\"@ref\":{\"id\":\"characters\",\"class\":{\"@ref\":{\"id\":\"classes\"}}}}}},\"params\":{\"object\":{\"password\":\"abracadabra\"}}}"

    val logout = Logout(true)
    json.writeValueAsString(logout) shouldBe "{\"logout\":true}"

    val identify = Identify(RefV("104979509695139637", RefV("characters", Native.Classes)), "abracadabra")
    json.writeValueAsString(identify) shouldBe "{\"identify\":{\"@ref\":{\"id\":\"104979509695139637\",\"class\":{\"@ref\":{\"id\":\"characters\",\"class\":{\"@ref\":{\"id\":\"classes\"}}}}}},\"password\":\"abracadabra\"}"

    val identity = Identity()
    json.writeValueAsString(identity) shouldBe "{\"identity\":null}"

    val hasIdentity = HasIdentity()
    json.writeValueAsString(hasIdentity) shouldBe "{\"has_identity\":null}"
  }

  it should "serialize date and time functions" in {
    val time = Time("1970-01-01T00:00:00+00:00")
    json.writeValueAsString(time) shouldBe "{\"time\":\"1970-01-01T00:00:00+00:00\"}"

    val epoch = Epoch(10, TimeUnit.Second)
    json.writeValueAsString(epoch) shouldBe "{\"epoch\":10,\"unit\":\"second\"}"

    val epoch2 = Epoch(10, "millisecond")
    json.writeValueAsString(epoch2) shouldBe "{\"epoch\":10,\"unit\":\"millisecond\"}"

    val date = query.Date("1970-01-02")
    json.writeValueAsString(date) shouldBe "{\"date\":\"1970-01-02\"}"
  }

  it should "serialize string functions" in {
    val concat = Concat(Arr("Hen", "Wen"))
    json.writeValueAsString(concat) shouldBe "{\"concat\":[\"Hen\",\"Wen\"]}"

    val concat2 = Concat(Arr("Hen", "Wen"), " ")
    json.writeValueAsString(concat2) shouldBe "{\"concat\":[\"Hen\",\"Wen\"],\"separator\":\" \"}"

    json.writeValueAsString(Casefold("Hen Wen")) shouldBe "{\"casefold\":\"Hen Wen\"}"
    json.writeValueAsString(Casefold("Hen Wen", "NFC")) shouldBe "{\"casefold\":\"Hen Wen\",\"normalizer\":\"NFC\"}"
    json.writeValueAsString(Casefold("Hen Wen", Normalizer.NFC)) shouldBe "{\"casefold\":\"Hen Wen\",\"normalizer\":\"NFC\"}"
  }

  it should "serialize misc and mathematical functions" in {
    val newId = NewId()
    json.writeValueAsString(newId) shouldBe "{\"new_id\":null}"

    val clazz = Class("spells")
    json.writeValueAsString(clazz) shouldBe "{\"class\":\"spells\"}"

    val database = Database("db-test")
    json.writeValueAsString(database) shouldBe "{\"database\":\"db-test\"}"

    val index = Index("spells_by_name")
    json.writeValueAsString(index) shouldBe "{\"index\":\"spells_by_name\"}"

    val equals = Equals("fire", "fire")
    json.writeValueAsString(equals) shouldBe "{\"equals\":[\"fire\",\"fire\"]}"

    val add = Add(1, 2)
    json.writeValueAsString(add) shouldBe "{\"add\":[1,2]}"

    val multiply = Multiply(1, 2)
    json.writeValueAsString(multiply) shouldBe "{\"multiply\":[1,2]}"

    val subtract = Subtract(1, 2)
    json.writeValueAsString(subtract) shouldBe "{\"subtract\":[1,2]}"

    val divide = Divide(1, 2)
    json.writeValueAsString(divide) shouldBe "{\"divide\":[1,2]}"

    val modulo = Modulo(1, 2)
    json.writeValueAsString(modulo) shouldBe "{\"modulo\":[1,2]}"

    val and = And(true, false)
    json.writeValueAsString(and) shouldBe "{\"and\":[true,false]}"

    val or = Or(true, false)
    json.writeValueAsString(or) shouldBe "{\"or\":[true,false]}"

    val not = Not(false)
    json.writeValueAsString(not) shouldBe "{\"not\":false}"
  }
}
