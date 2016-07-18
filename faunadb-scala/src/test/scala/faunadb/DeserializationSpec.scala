package faunadb

import com.fasterxml.jackson.databind.ObjectMapper
import faunadb.values._
import org.joda.time.DateTimeZone.UTC
import org.joda.time.{ Duration, Instant, LocalDate }
import org.scalatest.{ FlatSpec, Matchers }

class DeserializationSpec extends FlatSpec with Matchers {
  val json = new ObjectMapper()

  "Query AST deserialization" should "deserialize a query response with refs" in {
    val toDeserialize = """{
      "ref":{"@ref":"classes/spells/93044099947429888"},
      "class":{"@ref":"classes/spells"},
      "ts":1424992618413105,
      "data":{"refField":{"@ref":"classes/spells/93044099909681152"}}
    }"""

    val parsed = json.readValue[Value](toDeserialize, classOf[Value])

    parsed should equal (ObjectV(
      "ref" -> RefV("classes/spells/93044099947429888"),
      "class" -> RefV("classes/spells"),
      "ts" -> LongV(1424992618413105L),
      "data" -> ObjectV("refField" -> RefV("classes/spells/93044099909681152"))))
  }

  it should "deserialize a query response" in {
    val toDeserialize = """{
      "class":{"@ref":"classes/derp"},
      "data":{"test":1},
      "ref":{"@ref":"classes/derp/101192216816386048"},
      "ts":1432763268186882
    }"""
    val parsed = json.readValue(toDeserialize, classOf[Value])

    parsed should equal (ObjectV(
      "ref" -> RefV("classes/derp/101192216816386048"),
      "class" -> RefV("classes/derp"),
      "ts" -> LongV(1432763268186882L),
      "data" -> ObjectV("test" -> LongV(1))))
  }

  it should "deserialize a query response with a literal object" in {
    val toDeserialize = """{
      "class":{"@ref":"classes/derp"},
      "data":{"test":{"field1":{"@obj":{"@name":"Test"}}}},
      "ref":{"@ref":"classes/derp/101727203651223552"},
      "ts":1433273471399755
    }"""
    val parsed = json.readValue(toDeserialize, classOf[Value])

    parsed should equal (ObjectV(
      "ref" -> RefV("classes/derp/101727203651223552"),
      "class" -> RefV("classes/derp"),
      "ts" -> LongV(1433273471399755L),
      "data" -> ObjectV("test" -> ObjectV("field1" -> ObjectV("@name" -> StringV("Test"))))))
  }

  it should "deserialize empty object" in {
    val toDeserialize = """{}"""
    val parsed = json.readValue(toDeserialize, classOf[Value])

    parsed should equal (ObjectV.empty)
  }

  it should "deserialize ts" in {
    val toDeserialize = """{"@ts":"1970-01-01T00:05:00Z"}"""
    val parsed = json.readValue(toDeserialize, classOf[Value])

    parsed should equal (TimeV(new Instant(0).plus(Duration.standardMinutes(5))))
  }

  it should "deserialize date" in {
    val toDeserialize = """{"@date":"1970-01-03"}"""
    val parsed = json.readValue(toDeserialize, classOf[Value])

    parsed should equal (DateV(new LocalDate(0, UTC).plusDays(2)))
  }
}
