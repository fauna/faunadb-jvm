package faunadb

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import faunadb.query._
import faunadb.values._
import faunadb.values.time._
import org.joda.time.DateTimeZone.UTC
import org.joda.time.{ Duration, Instant, LocalDate }
import org.scalatest.{ FlatSpec, Matchers }

class SerializationSpec extends FlatSpec with Matchers {
  val json = new ObjectMapper()
  json.registerModule(new DefaultScalaModule)

  "Query AST serialization" should "serialize ref" in {
    val ref = Ref("some/ref")
    json.writeValueAsString(ref) shouldBe "{\"@ref\":\"some/ref\"}"
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

  it should "serialize complex values" in {
    json.writeValueAsString(Arr(1, "test")) shouldBe "[1,\"test\"]"
    json.writeValueAsString(Arr(Arr(Obj("test" -> "value"), 2323, true), "hi", Obj("test" -> "yo", "test2" -> NullV))) shouldBe "[[{\"object\":{\"test\":\"value\"}},2323,true],\"hi\",{\"object\":{\"test\":\"yo\",\"test2\":null}}]"
    json.writeValueAsString(Obj("test" -> 1, "test2" -> Ref("some/ref"))) shouldBe "{\"object\":{\"test\":1,\"test2\":{\"@ref\":\"some/ref\"}}}"
  }

  it should "serialize basic forms" in {
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
      Create(Ref("some/ref/1"), Obj("data" -> Obj("name" -> "Hen Wen"))),
      Get(Ref("some/ref/1")))
    json.writeValueAsString(doForm) shouldBe "{\"do\":[{\"create\":{\"@ref\":\"some/ref/1\"},\"params\":{\"object\":{\"data\":{\"object\":{\"name\":\"Hen Wen\"}}}}},{\"get\":{\"@ref\":\"some/ref/1\"}}]}"

    val select = Select("favorites" / "foods" / 1, Obj("favorites" -> Obj("foods" -> Arr("crunchings", "munchings", "lunchings"))))
    json.writeValueAsString(select) shouldBe "{\"select\":[\"favorites\",\"foods\",1],\"from\":{\"object\":{\"favorites\":{\"object\":{\"foods\":[\"crunchings\",\"munchings\",\"lunchings\"]}}}}}"

    val lambda1 = Lambda(a => a)
    json.writeValueAsString(lambda1) should equal ("""{"lambda":"a","expr":{"var":"a"}}""")

    val lambda2 = Lambda((a, b) => Arr(b, a))
    json.writeValueAsString(lambda2) should equal ("""{"lambda":["a","b"],"expr":[{"var":"b"},{"var":"a"}]}""")

    val lambda3 = Lambda((a, _, _) => a)
    json.writeValueAsString(lambda3) should equal ("""{"lambda":["a","_","_"],"expr":{"var":"a"}}""")

    val lambda4 = Lambda(Not(_))
    json.writeValueAsString(lambda4) should equal ("""{"lambda":"x$3","expr":{"not":{"var":"x$3"}}}""")
  }

  it should "serialize collections" in {
    val map = Map(Arr(1, 2, 3), Lambda(munchings => munchings))
    json.writeValueAsString(map) shouldBe "{\"map\":{\"lambda\":\"munchings\",\"expr\":{\"var\":\"munchings\"}},\"collection\":[1,2,3]}"

    val map2 = Map(Arr(1, 2, 3), Lambda("munchings", Var("munchings")))
    json.writeValueAsString(map2) shouldBe "{\"map\":{\"lambda\":\"munchings\",\"expr\":{\"var\":\"munchings\"}},\"collection\":[1,2,3]}"

    val foreach = Foreach(Arr(Ref("another/ref/1"), Ref("another/ref/2")), Lambda(creature => Create(Ref("some/ref"), Obj("data" -> Obj("some" -> creature)))))
    json.writeValueAsString(foreach) shouldBe "{\"foreach\":{\"lambda\":\"creature\",\"expr\":{\"create\":{\"@ref\":\"some/ref\"},\"params\":{\"object\":{\"data\":{\"object\":{\"some\":{\"var\":\"creature\"}}}}}}},\"collection\":[{\"@ref\":\"another/ref/1\"},{\"@ref\":\"another/ref/2\"}]}"

    val filter = Filter(Arr(1,2,3), Lambda(i => Equals(1, i)))
    json.writeValueAsString(filter) shouldBe "{\"filter\":{\"lambda\":\"i\",\"expr\":{\"equals\":[1,{\"var\":\"i\"}]}},\"collection\":[1,2,3]}"

    val take = Take(2, Arr(1,2,3))
    json.writeValueAsString(take) shouldBe "{\"take\":2,\"collection\":[1,2,3]}"

    val drop = Drop(2, Arr(1,2,3))
    json.writeValueAsString(drop) shouldBe "{\"drop\":2,\"collection\":[1,2,3]}"

    val prepend = Prepend(Arr(4,5,6), Arr(1,2,3))
    json.writeValueAsString(prepend) shouldBe "{\"prepend\":[1,2,3],\"collection\":[4,5,6]}"

    val append = Append(Arr(1,2,3), Arr(4,5,6))
    json.writeValueAsString(append) shouldBe "{\"append\":[4,5,6],\"collection\":[1,2,3]}"
  }

  it should "serialize resource retrievals" in {
    val ref = Ref("some/ref/1")
    val get = Get(ref)
    json.writeValueAsString(get) shouldBe "{\"get\":{\"@ref\":\"some/ref/1\"}}"

    val paginate1 = Paginate(Union(Match(Ref("indexes/some_index"), "term"), Match(Ref("indexes/some_index"), "term2")))
    json.writeValueAsString(paginate1) shouldBe "{\"paginate\":{\"union\":[{\"match\":\"term\",\"index\":{\"@ref\":\"indexes/some_index\"}},{\"match\":\"term2\",\"index\":{\"@ref\":\"indexes/some_index\"}}]}}"

    val paginate2 = Paginate(Union(Match(Ref("indexes/some_index"), "term"), Match(Ref("indexes/some_index"), "term2")), sources = true)
    json.writeValueAsString(paginate2) shouldBe "{\"paginate\":{\"union\":[{\"match\":\"term\",\"index\":{\"@ref\":\"indexes/some_index\"}},{\"match\":\"term2\",\"index\":{\"@ref\":\"indexes/some_index\"}}]},\"sources\":true}"

    val paginate3 = Paginate(Union(Match(Ref("indexes/some_index"), "term"), Match(Ref("indexes/some_index"), "term2")), events = true)
    json.writeValueAsString(paginate3) shouldBe "{\"paginate\":{\"union\":[{\"match\":\"term\",\"index\":{\"@ref\":\"indexes/some_index\"}},{\"match\":\"term2\",\"index\":{\"@ref\":\"indexes/some_index\"}}]},\"events\":true}"

    val paginate4 = Paginate(Union(Match(Ref("indexes/some_index"), "term"), Match(Ref("indexes/some_index"), "term2")), Before(Ref("some/ref/1")), size = 4)
    json.writeValueAsString(paginate4) shouldBe "{\"paginate\":{\"union\":[{\"match\":\"term\",\"index\":{\"@ref\":\"indexes/some_index\"}},{\"match\":\"term2\",\"index\":{\"@ref\":\"indexes/some_index\"}}]},\"before\":{\"@ref\":\"some/ref/1\"},\"size\":4}"
  }

  it should "serialize resource modifications" in {
    val create = Create(Ref("classes/spells"),
      Obj("data" -> Obj(
        "name" -> "Mountainous Thunder",
        "element" -> "air",
        "cost" -> 15)))

    json.writeValueAsString(create) shouldBe "{\"create\":{\"@ref\":\"classes/spells\"},\"params\":{\"object\":{\"data\":{\"object\":{\"name\":\"Mountainous Thunder\",\"element\":\"air\",\"cost\":15}}}}}"

    val update = Update(Ref("classes/spells/123456"),
      Obj("data" -> Obj(
        "name" -> "Mountain's Thunder",
        "cost" -> NullV)))

    json.writeValueAsString(update) shouldBe "{\"update\":{\"@ref\":\"classes/spells/123456\"},\"params\":{\"object\":{\"data\":{\"object\":{\"name\":\"Mountain's Thunder\",\"cost\":null}}}}}"

    val replace = Replace(Ref("classes/spells/123456"),
      Obj("data" -> Obj(
        "name" -> "Mountain's Thunder",
        "element" -> Arr("air", "earth"),
        "cost" -> 10)))

    json.writeValueAsString(replace) shouldBe "{\"replace\":{\"@ref\":\"classes/spells/123456\"},\"params\":{\"object\":{\"data\":{\"object\":{\"name\":\"Mountain's Thunder\",\"element\":[\"air\",\"earth\"],\"cost\":10}}}}}"

    val delete = Delete(Ref("classes/spells/123456"))
    json.writeValueAsString(delete) shouldBe "{\"delete\":{\"@ref\":\"classes/spells/123456\"}}"

    val insert = Insert(Ref("classes/spells/123456"), 1L, Action.Create, Obj("data" -> Obj("name" -> "Mountain's Thunder", "cost" -> 10, "element" -> Arr("air", "earth"))))
    json.writeValueAsString(insert) shouldBe "{\"insert\":{\"@ref\":\"classes/spells/123456\"},\"ts\":1,\"action\":\"create\",\"params\":{\"object\":{\"data\":{\"object\":{\"name\":\"Mountain's Thunder\",\"cost\":10,\"element\":[\"air\",\"earth\"]}}}}}"

    val remove = Remove(Ref("classes/spells/123456"), 1L, Action.Delete)
    json.writeValueAsString(remove) shouldBe "{\"remove\":{\"@ref\":\"classes/spells/123456\"},\"ts\":1,\"action\":\"delete\"}"
  }

  it should "serialize sets" in {
    val matchSet = Match(Ref("indexes/spells_by_elements"), "fire")
    json.writeValueAsString(matchSet) shouldBe "{\"match\":\"fire\",\"index\":{\"@ref\":\"indexes/spells_by_elements\"}}"

    val union = Union(Match(Ref("indexes/spells_by_element"), "fire"), Match(Ref("indexes/spells_by_element"), "water"))
    json.writeValueAsString(union) shouldBe "{\"union\":[{\"match\":\"fire\",\"index\":{\"@ref\":\"indexes/spells_by_element\"}},{\"match\":\"water\",\"index\":{\"@ref\":\"indexes/spells_by_element\"}}]}"

    val intersection = Intersection(Match(Ref("indexes/spells_by_element"), "fire"), Match(Ref("indexes/spells_by_element"), "water"))
    json.writeValueAsString(intersection) shouldBe "{\"intersection\":[{\"match\":\"fire\",\"index\":{\"@ref\":\"indexes/spells_by_element\"}},{\"match\":\"water\",\"index\":{\"@ref\":\"indexes/spells_by_element\"}}]}"

    val difference = Difference(Match(Ref("indexes/spells_by_element"), "fire"), Match(Ref("indexes/spells_by_element"), "water"))
    json.writeValueAsString(difference) shouldBe "{\"difference\":[{\"match\":\"fire\",\"index\":{\"@ref\":\"indexes/spells_by_element\"}},{\"match\":\"water\",\"index\":{\"@ref\":\"indexes/spells_by_element\"}}]}"

    val join = Join(Match(Ref("indexes/spells_by_element"), "fire"), Lambda(spell => Get(spell)))
    json.writeValueAsString(join) shouldBe "{\"join\":{\"match\":\"fire\",\"index\":{\"@ref\":\"indexes/spells_by_element\"}},\"with\":{\"lambda\":\"spell\",\"expr\":{\"get\":{\"var\":\"spell\"}}}}"
  }

  it should "serialize date and ts" in {
    val ts = TimeV(new Instant(0).plus(Duration.standardMinutes(5)))
    json.writeValueAsString(ts) shouldBe "{\"@ts\":\"1970-01-01T00:05:00.000000000Z\"}"

    val withMillis = TimeV(HighPrecisionTime(new Instant(1)))
    json.writeValueAsString(withMillis) shouldBe "{\"@ts\":\"1970-01-01T00:00:00.001000000Z\"}"

    val withMicros = TimeV(HighPrecisionTime(new Instant(1), microsToAdd=1001))
    json.writeValueAsString(withMicros) shouldBe "{\"@ts\":\"1970-01-01T00:00:00.002001000Z\"}"

    val withNanos = TimeV(HighPrecisionTime(new Instant(1), nanosToAdd=1001))
    json.writeValueAsString(withNanos) shouldBe "{\"@ts\":\"1970-01-01T00:00:00.001001001Z\"}"

    val microsOverflow = TimeV(HighPrecisionTime(new Instant(1000), microsToAdd=1777777777))
    json.writeValueAsString(microsOverflow) shouldBe "{\"@ts\":\"1970-01-01T00:29:38.777777000Z\"}"

    val nanosOverflow = TimeV(HighPrecisionTime(new Instant(1000), nanosToAdd=1999999999))
    json.writeValueAsString(nanosOverflow) shouldBe "{\"@ts\":\"1970-01-01T00:00:02.999999999Z\"}"

    val microAndNanosOverflow = TimeV(HighPrecisionTime(new Instant(1000), microsToAdd = 1000000, nanosToAdd = 1000000000))
    json.writeValueAsString(microAndNanosOverflow) shouldBe "{\"@ts\":\"1970-01-01T00:00:03.000000000Z\"}"

    val date = DateV(new LocalDate(0, UTC).plusDays(2))
    json.writeValueAsString(date) shouldBe "{\"@date\":\"1970-01-03\"}"
  }

  it should "serialize authentication functions" in {
    val login = Login(Ref("classes/characters/104979509695139637"), Obj("password" -> "abracadabra"))
    json.writeValueAsString(login) shouldBe "{\"login\":{\"@ref\":\"classes/characters/104979509695139637\"},\"params\":{\"object\":{\"password\":\"abracadabra\"}}}"

    val logout = Logout(true)
    json.writeValueAsString(logout) shouldBe "{\"logout\":true}"

    val identify = Identify(Ref("classes/characters/104979509695139637"), "abracadabra")
    json.writeValueAsString(identify) shouldBe "{\"identify\":{\"@ref\":\"classes/characters/104979509695139637\"},\"password\":\"abracadabra\"}"
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

  it should "serialize misc and mathematical functions" in {
    val nextId = NextId()
    json.writeValueAsString(nextId) shouldBe "{\"next_id\":null}"

    val clazz = Clazz("spells")
    json.writeValueAsString(clazz) shouldBe "{\"class\":\"spells\"}"

    val database = Database("db-test")
    json.writeValueAsString(database) shouldBe "{\"database\":\"db-test\"}"

    val equals = Equals("fire", "fire")
    json.writeValueAsString(equals) shouldBe "{\"equals\":[\"fire\",\"fire\"]}"

    val concat = Concat(Arr("Hen", "Wen"))
    json.writeValueAsString(concat) shouldBe "{\"concat\":[\"Hen\",\"Wen\"]}"

    val concat2 = Concat(Arr("Hen", "Wen"), " ")
    json.writeValueAsString(concat2) shouldBe "{\"concat\":[\"Hen\",\"Wen\"],\"separator\":\" \"}"

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
