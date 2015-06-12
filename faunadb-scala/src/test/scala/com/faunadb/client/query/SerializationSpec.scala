package com.faunadb.client.query

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.scalatest.{FlatSpec, Matchers}
import Path._
import Values._

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
    json.writeValueAsString(NumberV(1234)) shouldBe "1234"
    json.writeValueAsString(NumberV(Long.MaxValue)) shouldBe Long.MaxValue.toString
    json.writeValueAsString(DoubleV(1.234)) shouldBe "1.234"
    json.writeValueAsString(NullV) shouldBe "null"
  }

  it should "serialize complex values" in {
    json.writeValueAsString(ArrayV(1, "test")) shouldBe "[1,\"test\"]"
    json.writeValueAsString(ArrayV(ArrayV(ObjectV("test" -> "value"), 2323, true), "hi", ObjectV("test" -> "yo", "test2" -> NullV))) shouldBe "[[{\"object\":{\"test\":\"value\"}},2323,true],\"hi\",{\"object\":{\"test\":\"yo\",\"test2\":null}}]"
    json.writeValueAsString(ObjectV("test" -> 1, "test2" -> Ref("some/ref"))) shouldBe "{\"object\":{\"test\":1,\"test2\":{\"@ref\":\"some/ref\"}}}"
  }

  it should "serialize basic forms" in {
    val let = Let(scala.collection.Map[String, Value]("x" -> 1, "y" -> "2"), Var("x"))
    json.writeValueAsString(let) shouldBe "{\"let\":{\"x\":1,\"y\":\"2\"},\"in\":{\"var\":\"x\"}}"

    val ifForm = If(true, "was true", "was false")
    json.writeValueAsString(ifForm) shouldBe "{\"if\":true,\"then\":\"was true\",\"else\":\"was false\"}"

    val doForm = Do(Seq(
      Create(Ref("some/ref/1"), ObjectV("data" -> ObjectV("name" -> "Hen Wen"))),
      Get(Ref("some/ref/1"))))
    json.writeValueAsString(doForm) shouldBe "{\"do\":[{\"create\":{\"@ref\":\"some/ref/1\"},\"params\":{\"object\":{\"data\":{\"object\":{\"name\":\"Hen Wen\"}}}}},{\"get\":{\"@ref\":\"some/ref/1\"}}]}"

    val select = Select(Seq("favorites", "foods", 1), ObjectV("favorites" -> ObjectV("foods" -> ArrayV("crunchings", "munchings", "lunchings"))))
    json.writeValueAsString(select) shouldBe "{\"select\":[\"favorites\",\"foods\",1],\"from\":{\"object\":{\"favorites\":{\"object\":{\"foods\":[\"crunchings\",\"munchings\",\"lunchings\"]}}}}}"
  }

  it should "serialize collections" in {
    val map = Map(Lambda("munchings", Var("munchings")), ArrayV(1, 2, 3))
    json.writeValueAsString(map) shouldBe "{\"map\":{\"lambda\":\"munchings\",\"expr\":{\"var\":\"munchings\"}},\"collection\":[1,2,3]}"

    val foreach = Foreach(Lambda("creature", Create(Ref("some/ref"), ObjectV("data" -> ObjectV("some" -> Var("creature"))))), ArrayV(Ref("another/ref/1"), Ref("another/ref/2")))
    json.writeValueAsString(foreach) shouldBe "{\"foreach\":{\"lambda\":\"creature\",\"expr\":{\"create\":{\"@ref\":\"some/ref\"},\"params\":{\"object\":{\"data\":{\"object\":{\"some\":{\"var\":\"creature\"}}}}}}},\"collection\":[{\"@ref\":\"another/ref/1\"},{\"@ref\":\"another/ref/2\"}]}"
  }

  it should "serialize a get and paginate" in {
    val ref = Ref("some/ref")
    val get = Get(ref)
    json.writeValueAsString(get) shouldBe "{\"get\":{\"@ref\":\"some/ref\"}}"

    val get2 = Paginate(ref, cursor = Some(Before(Ref("another/ref"))))
    json.writeValueAsString(get2) shouldBe "{\"paginate\":{\"@ref\":\"some/ref\"},\"before\":{\"@ref\":\"another/ref\"}}"

    val get3 = Paginate(ref, ts = Some(1234), cursor = Some(After(Ref("another/ref"))), size = Some(1000))
    json.writeValueAsString(get3) shouldBe "{\"paginate\":{\"@ref\":\"some/ref\"},\"ts\":1234,\"after\":{\"@ref\":\"another/ref\"},\"size\":1000}"
  }

  it should "serialize a match" in {
    val m = Match("testTerm", Ref("some/index"))
    json.writeValueAsString(m) shouldBe "{\"match\":\"testTerm\",\"index\":{\"@ref\":\"some/index\"}}"
  }

  it should "serialize a complex set" in {
    val setTerm1 = Match("testTerm1", Ref("some/index"))
    val setTerm2 = Match("testTerm2", Ref("another/index"))

    val union = Union(Array(setTerm1, setTerm2))
    json.writeValueAsString(union) shouldBe "{\"union\":[{\"match\":\"testTerm1\",\"index\":{\"@ref\":\"some/index\"}},{\"match\":\"testTerm2\",\"index\":{\"@ref\":\"another/index\"}}]}"

    val intersection = Intersection(Array(setTerm1, setTerm2))
    json.writeValueAsString(intersection) shouldBe "{\"intersection\":[{\"match\":\"testTerm1\",\"index\":{\"@ref\":\"some/index\"}},{\"match\":\"testTerm2\",\"index\":{\"@ref\":\"another/index\"}}]}"

    val difference = Difference(Array(setTerm1, setTerm2))
    json.writeValueAsString(difference) shouldBe "{\"difference\":[{\"match\":\"testTerm1\",\"index\":{\"@ref\":\"some/index\"}},{\"match\":\"testTerm2\",\"index\":{\"@ref\":\"another/index\"}}]}"

    val join = Join(setTerm1, "some/target/_")
    json.writeValueAsString(join) shouldBe "{\"join\":{\"match\":\"testTerm1\",\"index\":{\"@ref\":\"some/index\"}},\"with\":\"some/target/_\"}"
  }

  it should "serialize events" in {
    val ref = Ref("some/ref")
    val events = Events(ref)

    json.writeValueAsString(events) shouldBe "{\"events\":{\"@ref\":\"some/ref\"}}"

    val events2 = Events(ref, cursor=Some(Before(Ref("another/ref"))), size=Some(50))
    json.writeValueAsString(events2) shouldBe "{\"events\":{\"@ref\":\"some/ref\"},\"before\":{\"@ref\":\"another/ref\"},\"size\":50}"
  }

  it should "serialize a resource operation" in {
    val ref = Ref("some/ref")
    val params = collection.Map[String, Value]("test1" -> "value2")
    val create = Create(ref, params)
    json.writeValueAsString(create) shouldBe "{\"create\":{\"@ref\":\"some/ref\"},\"params\":{\"object\":{\"test1\":\"value2\"}}}"

    val put = Replace(ref, params)
    json.writeValueAsString(put) shouldBe "{\"replace\":{\"@ref\":\"some/ref\"},\"params\":{\"object\":{\"test1\":\"value2\"}}}"

    val patch = Update(ref, params)
    json.writeValueAsString(patch) shouldBe "{\"update\":{\"@ref\":\"some/ref\"},\"params\":{\"object\":{\"test1\":\"value2\"}}}"

    val delete = Delete(ref)
    json.writeValueAsString(delete) shouldBe "{\"delete\":{\"@ref\":\"some/ref\"}}"
  }
}
