package com.faunadb.query

import com.fasterxml.jackson.databind.ObjectMapper
import org.scalatest.{FlatSpec, Matchers}

class SerializationSpec extends FlatSpec with Matchers {
  val json = new ObjectMapper()

  "Query AST serialization" should "serialize ref" in {
    val ref = Ref("some/ref")
    json.writeValueAsString(ref) shouldBe "{\"@ref\":\"some/ref\"}"
  }

  it should "serialize a Get" in {
    val ref = Ref("some/ref")
    val get = Get(ref)
    json.writeValueAsString(get) shouldBe "{\"get\":{\"@ref\":\"some/ref\"}}"

    val get2 = Get(ref, cursor = Some(Before(Ref("another/ref"))))
    json.writeValueAsString(get2) shouldBe "{\"get\":{\"@ref\":\"some/ref\"},\"before\":{\"@ref\":\"another/ref\"}}"

    val get3 = Get(ref, ts = Some(1234), cursor = Some(After(Ref("another/ref"))), size = Some(1000))
    json.writeValueAsString(get3) shouldBe "{\"get\":{\"@ref\":\"some/ref\"},\"ts\":1234,\"after\":{\"@ref\":\"another/ref\"},\"size\":1000}"
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
    import Primitives._

    val obj = ObjectPrimitive(Map("test1" -> "value1", "test2" -> 2, "test3" -> true))
    json.writeValueAsString(obj) shouldBe "{\"object\":{\"test1\":\"value1\",\"test2\":2,\"test3\":true}}"

    val nestedObj = ObjectPrimitive(Map("test1" -> ObjectPrimitive(Map("nested1" -> "nestedValue1"))))
    json.writeValueAsString(nestedObj) shouldBe "{\"object\":{\"test1\":{\"object\":{\"nested1\":\"nestedValue1\"}}}}"
  }

  it should "serialize a resource operation" in {
    import Primitives._
    val ref = Ref("some/ref")
    val params = Map[String, Primitive]("test1" -> "value2")
    val create = Create(ref, params)
    json.writeValueAsString(create) shouldBe "{\"create\":{\"@ref\":\"some/ref\"},\"params\":{\"object\":{\"test1\":\"value2\"}}}"

    val put = Put(ref, params)
    json.writeValueAsString(put) shouldBe "{\"put\":{\"@ref\":\"some/ref\"},\"params\":{\"object\":{\"test1\":\"value2\"}}}"

    val patch = Patch(ref, params)
    json.writeValueAsString(patch) shouldBe "{\"patch\":{\"@ref\":\"some/ref\"},\"params\":{\"object\":{\"test1\":\"value2\"}}}"

    val delete = Delete(ref)
    json.writeValueAsString(delete) shouldBe "{\"delete\":{\"@ref\":\"some/ref\"}}"
  }

  it should "serialize a complex expression" in {
    val ref = Ref("some/ref")
    val expr1 = Create(ref, ObjectPrimitive(Map()))
    val expr2 = Create(ref, ObjectPrimitive(Map()))

    val complex = Do(Array(expr1, expr2))
    json.writeValueAsString(complex) shouldBe "{\"do\":[{\"create\":{\"@ref\":\"some/ref\"},\"params\":{\"object\":{}}},{\"create\":{\"@ref\":\"some/ref\"},\"params\":{\"object\":{}}}]}"
  }
}
