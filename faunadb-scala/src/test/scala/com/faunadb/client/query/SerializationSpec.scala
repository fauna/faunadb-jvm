package com.faunadb.client.query

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.scalatest.{FlatSpec, Matchers}
import Values._

class SerializationSpec extends FlatSpec with Matchers {
  val json = new ObjectMapper()
  json.registerModule(new DefaultScalaModule)
  json.registerModule(new SimpleModule().setDeserializerModifier(new FaunaDeserializerModifier))

  "Query AST serialization" should "serialize ref" in {
    val ref = Ref("some/ref")
    json.writeValueAsString(ref) shouldBe "{\"@ref\":\"some/ref\"}"
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

  it should "serialize object primitives" in {
    val obj = ObjectV("test1" -> "value1", "test2" -> 2, "test3" -> true)
    json.writeValueAsString(obj) shouldBe "{\"object\":{\"test1\":\"value1\",\"test2\":2,\"test3\":true}}"

    val nestedObj = ObjectV("test1" -> ObjectV("nested1" -> "nestedValue1"))
    json.writeValueAsString(nestedObj) shouldBe "{\"object\":{\"test1\":{\"object\":{\"nested1\":\"nestedValue1\"}}}}"
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

  it should "serialize a complex expression" in {
    val ref = Ref("some/ref")
    val expr1 = Create(ref, ObjectV.empty)
    val expr2 = Create(ref, ObjectV.empty)

    val complex = Do(Array(expr1, expr2))
    json.writeValueAsString(complex) shouldBe "{\"do\":[{\"create\":{\"@ref\":\"some/ref\"},\"params\":{\"object\":{}}},{\"create\":{\"@ref\":\"some/ref\"},\"params\":{\"object\":{}}}]}"
  }
}
