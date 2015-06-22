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

  it should "deserialize a database response" in {
    val toDeserialize = "{\n" +
      "        \"class\": {\n" +
      "            \"@ref\": \"databases\"\n" +
      "        },\n" +
      "        \"name\": \"spells\",\n" +
      "        \"ref\": {\n" +
      "            \"@ref\": \"databases/spells\"\n" +
      "        },\n" +
      "        \"ts\": 1434343547025544\n" +
      "    }\n";

    val parsed = json.readValue(toDeserialize, classOf[ResponseNode])
    val database = parsed.asDatabase
    database.name shouldBe "spells"
    database.classRef shouldBe Ref("databases")
    database.ts shouldBe 1434343547025544L
    database.ref shouldBe Ref("databases/spells")
  }

  it should "deserialize a key response" in {
    val toDeserialize = " {\n" +
      "        \"class\": {\n" +
      "            \"@ref\": \"keys\"\n" +
      "        },\n" +
      "        \"data\": {\n" +
      "            \"data\": \"yeah\",\n" +
      "            \"some\": 123\n" +
      "        },\n" +
      "        \"database\": {\n" +
      "            \"@ref\": \"databases/spells\"\n" +
      "        },\n" +
      "        \"hashed_secret\": \"$2a$05$LKJiF.hpkt40W9oMC/5atu2g03m2.cPGU9Srys5vmAdOgBaGYjfl2\",\n" +
      "        \"ref\": {\n" +
      "            \"@ref\": \"keys/102850208874889216\"\n" +
      "        },\n" +
      "        \"role\": \"server\",\n" +
      "        \"secret\": \"kqoBbWW4VRAAAAACtCcfczgIhDni0TUjuk5RxoNwpgzx\",\n" +
      "        \"ts\": 1434344452631179\n" +
      "    }\n";

    val parsed = json.readValue(toDeserialize, classOf[ResponseNode])
    val key = parsed.asKey
    key.classRef shouldBe Ref("keys")
    key.database shouldBe Ref("databases/spells")
    key.data("some").asNumber shouldBe 123L
    key.data("data").asString shouldBe "yeah"
    key.hashedSecret shouldBe "$2a$05$LKJiF.hpkt40W9oMC/5atu2g03m2.cPGU9Srys5vmAdOgBaGYjfl2"
    key.ref shouldBe Ref("keys/102850208874889216")
    key.role shouldBe "server"
    key.secret shouldBe "kqoBbWW4VRAAAAACtCcfczgIhDni0TUjuk5RxoNwpgzx"
    key.ts shouldBe 1434344452631179L
  }

  it should "deserialize class response" in {
    val toDeserialize = " {\n" +
        "        \"class\": {\n" +
        "            \"@ref\": \"classes\"\n" +
        "        },\n" +
        "        \"history_days\": 30,\n" +
        "        \"name\": \"spells\",\n" +
        "        \"ref\": {\n" +
        "            \"@ref\": \"classes/spells\"\n" +
        "        },\n" +
        "        \"ts\": 1434344944425065\n" +
        "    }"

    val parsed = json.readValue(toDeserialize, classOf[ResponseNode])
    val cls = parsed.asClass
    cls.classRef shouldBe Ref("classes")
    cls.historyDays shouldBe 30L
    cls.name shouldBe "spells"
    cls.ref shouldBe Ref("classes/spells")
    cls.ts shouldBe 1434344944425065L
  }

  it should "deserialize index response" in {
    val toDeserialize = "{\n" +
      "        \"active\": false,\n" +
      "        \"class\": {\n" +
      "            \"@ref\": \"indexes\"\n" +
      "        },\n" +
      "        \"name\": \"spells_by_name\",\n" +
      "        \"ref\": {\n" +
      "            \"@ref\": \"indexes/spells_by_name\"\n" +
      "        },\n" +
      "        \"source\": {\n" +
      "            \"@ref\": \"classes/spells\"\n" +
      "        },\n" +
      "        \"terms\": [\n" +
      "            {\n" +
      "                \"path\": \"data.name\"\n" +
      "            }\n" +
      "        ],\n" +
      "        \"ts\": 1434345216167501,\n" +
      "        \"unique\": true\n" +
      "    }"

    val parsed = json.readValue(toDeserialize, classOf[ResponseNode])

    val idx = parsed.asIndex
    idx.active shouldBe false
    idx.classRef shouldBe Ref("indexes")
    idx.name shouldBe "spells_by_name"
    idx.ref shouldBe Ref("indexes/spells_by_name")
    idx.source shouldBe Ref("classes/spells")
    idx.terms shouldBe Seq(immutable.Map("path" -> "data.name"))
    idx.ts shouldBe 1434345216167501L
    idx.unique shouldBe true
  }

  it should "deserialize event response" in {
    val toDeserialize = "{\n" +
      "\t\t\t\"ts\": 1434477366352519,\n" +
      "\t\t\t\"action\": \"create\",\n" +
      "\t\t\t\"resource\": {\n" +
      "\t\t\t\t\"@ref\": \"classes/spells/102989579003363328\"\n" +
      "\t\t\t}\n" +
      "\t\t}"

    val parsed = json.readValue(toDeserialize, classOf[ResponseNode])
    val event = parsed.asEvent

    event.resource shouldBe Ref("classes/spells/102989579003363328")
    event.action shouldBe "create"
    event.ts shouldBe 1434477366352519L
  }

  it should "deserialize page response with no before or after" in {
    val toDeserialize = "{\n" +
      "        \"data\": [\n" +
      "            {\n" +
      "                \"@ref\": \"classes/spells/102851646450565120\"\n" +
      "            }\n" +
      "        ]\n" +
      "    }\n"

    val parsed = json.readValue(toDeserialize, classOf[ResponseNode])
    val page = parsed.asPage
    page.data.size shouldBe 1
    page.data(0).asRef shouldBe Ref("classes/spells/102851646450565120")
    page.after shouldBe None
    page.before shouldBe None
  }

  it should "deserialize page response with before" in {
    val toDeserialize = "{\n" +
      "        \"before\": {\n" +
      "            \"@ref\": \"classes/spells/102851646450565120\"\n" +
      "        },\n" +
      "        \"data\": [\n" +
      "            {\n" +
      "                \"@ref\": \"classes/spells/102851646450565120\"\n" +
      "            }\n" +
      "        ]\n" +
      "    }"

    val parsed = json.readValue(toDeserialize, classOf[ResponseNode])
    val page = parsed.asPage
    page.data.size shouldBe 1
    page.data(0).asRef shouldBe Ref("classes/spells/102851646450565120")
    page.before.get.asRef shouldBe Ref("classes/spells/102851646450565120")
    page.after shouldBe None
  }

  it should "deserialize page response with before and after" in {
    val toDeserialize = "{\n" +
      "        \"after\": {\n" +
      "            \"@ref\": \"classes/spells/102852248441192448\"\n" +
      "        },\n" +
      "        \"before\": {\n" +
      "            \"@ref\": \"classes/spells/102851646450565120\"\n" +
      "        },\n" +
      "        \"data\": [\n" +
      "            {\n" +
      "                \"@ref\": \"classes/spells/102851646450565120\"\n" +
      "            }\n" +
      "        ]\n" +
      "    }"

    val parsed = json.readValue(toDeserialize, classOf[ResponseNode])
    val page = parsed.asPage
    page.data.size shouldBe 1
    page.data(0).asRef shouldBe Ref("classes/spells/102851646450565120")
    page.before.get.asRef shouldBe Ref("classes/spells/102852248441192448")
    page.after.get.asRef shouldBe Ref("classes/spells/102851646450565120")
  }

  it should "deserialize page response with after" in {
    val toDeserialize = "{\n" +
      "        \"after\": {\n" +
      "            \"@ref\": \"classes/spells/102851646450565120\"\n" +
      "        },\n" +
      "        \"data\": [\n" +
      "            {\n" +
      "                \"@ref\": \"classes/spells/102851640310104064\"\n" +
      "            }\n" +
      "        ]\n" +
      "    }"

    val parsed = json.readValue(toDeserialize, classOf[ResponseNode])
    val page = parsed.asPage
    page.data.size shouldBe 1
    page.data(0).asRef shouldBe Ref("classes/spells/102851640310104064")
    page.before shouldBe None
    page.after.get.asRef shouldBe Ref("classes/spells/102851646450565120")
  }
}
