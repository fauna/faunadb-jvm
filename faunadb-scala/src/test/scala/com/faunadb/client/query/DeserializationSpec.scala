package com.faunadb.client.query

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.faunadb.client.response.{ResponseMap, Instance, ResponseNode}
import org.scalatest.{FlatSpec, Matchers}

import scala.collection.immutable

class DeserializationSpec extends FlatSpec with Matchers {
  val json = new ObjectMapper()
  val module = new SimpleModule()
//  module.addDeserializer[scala.collection.immutable.Map[_,_]](classOf[scala.collection.immutable.Map[_,_]],
//    (new ObjectDeserializer).asInstanceOf[JsonDeserializer[scala.collection.immutable.Map[_,_]]])

  json.registerModule(new DefaultScalaModule)
  json.registerModule(module)

  def toResponseNode(ref: Ref) = {
    new ResponseNode(json.valueToTree(ref), json)
  }

  def toResponseNode(value: Long) = {
    new ResponseNode(json.valueToTree(value), json)
  }

  def toResponseNode(value: String) = {
    new ResponseNode(json.valueToTree(value), json)
  }

  def toResponseNode(value: immutable.Map[String, String]) = {
    new ResponseNode(json.valueToTree(value), json)
  }

  "Query AST deserialization" should "deserialize a query response with refs" in {
    val toDeserialize = "{\n\t\t\"ref\": {\n\t\t\t\"@ref\": \"classes/spells/93044099947429888\"\n\t\t},\n\t\t\"class\": {\n\t\t\t\"@ref\": \"classes/spells\"\n\t\t},\n\t\t\"ts\": 1424992618413105,\n\t\t\"data\": {\n\t\t\t\"refField\": {\n\t\t\t\t\"@ref\": \"classes/spells/93044099909681152\"\n\t\t\t}\n\t\t}\n\t}"
    val parsed = json.readValue[ResponseNode](toDeserialize, classOf[ResponseNode])
    parsed.asInstance shouldBe Instance(Ref("classes/spells/93044099947429888"), Ref("classes/spells"), 1424992618413105L, new ResponseMap(immutable.Map("refField" -> toResponseNode(Ref("classes/spells/93044099909681152")))))
  }

  it should "deserialize a query response" in {
    val toDeserialize = "{\n        \"class\": {\n            \"@ref\": \"classes/derp\"\n        },\n        \"data\": {\n            \"test\": 1\n        },\n        \"ref\": {\n            \"@ref\": \"classes/derp/101192216816386048\"\n        },\n        \"ts\": 1432763268186882\n    }"
    val parsed = json.readValue(toDeserialize, classOf[ResponseNode])
    parsed.asInstance shouldBe Instance(Ref("classes/derp/101192216816386048"), Ref("classes/derp"), 1432763268186882L, new ResponseMap(immutable.Map("test" -> toResponseNode(1L))))
  }

  it should "deserialize a query response with a literal object" in {
    val toDeserialize = "{\n\"class\": {\n\"@ref\": \"classes/derp\"\n},\n\"data\": {\n\"test\": {\n\"field1\": {\n\"@obj\": {\n\"@name\": \"Test\"\n}\n}\n}\n},\n\"ref\": {\n\"@ref\": \"classes/derp/101727203651223552\"\n},\n\"ts\": 1433273471399755\n}"
    val parsed = json.readValue(toDeserialize, classOf[ResponseNode])
    val instance = parsed.asInstance
    val unwrappedField = instance.data("test").asObject("field1").asObject
    unwrappedField shouldBe immutable.Map("@name" -> toResponseNode("Test"))
  }
}
