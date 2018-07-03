package faunadb

import com.fasterxml.jackson.databind.{ JsonMappingException, ObjectMapper }
import faunadb.values._
import java.time.{ Instant, LocalDate }
import java.time.ZoneOffset.UTC
import java.time.temporal.ChronoUnit
import org.scalatest.{ FlatSpec, Matchers }

class DeserializationSpec extends FlatSpec with Matchers {
  val json = new ObjectMapper()

  "Query AST deserialization" should "deserialize a query response with refs" in {
    val toDeserialize = """{
      "ref":{"@ref":{"id":"93044099947429888","class":{"@ref":{"id":"spells","class":{"@ref":{"id":"classes"}}}}}},
      "class":{"@ref":{"id":"spells","class":{"@ref":{"id":"classes"}}}},
      "ts":1424992618413105,
      "data":{"refField":{"@ref":{"id":"93044099909681152","class":{"@ref":{"id":"spells","class":{"@ref":{"id":"classes"}}}}}}}
    }"""

    val parsed = json.readValue[Value](toDeserialize, classOf[Value])

    parsed should equal (ObjectV(
      "ref" -> RefV("93044099947429888", RefV("spells", Native.Classes)),
      "class" -> RefV("spells", Native.Classes),
      "ts" -> LongV(1424992618413105L),
      "data" -> ObjectV("refField" -> RefV("93044099909681152", RefV("spells", Native.Classes)))))
  }

  it should "deserialize a query response" in {
    val toDeserialize = """{
      "class":{"@ref":{"id":"derp","class":{"@ref":{"id":"classes"}}}},
      "data":{"test":1},
      "ref":{"@ref":{"id":"101192216816386048","class":{"@ref":{"id":"derp","class":{"@ref":{"id":"classes"}}}}}},
      "ts":1432763268186882
    }"""
    val parsed = json.readValue(toDeserialize, classOf[Value])

    parsed should equal (ObjectV(
      "ref" -> RefV("101192216816386048", RefV("derp", Native.Classes)),
      "class" -> RefV("derp", Native.Classes),
      "ts" -> LongV(1432763268186882L),
      "data" -> ObjectV("test" -> LongV(1))))
  }

  it should "deserialize a query response with a literal object" in {
    val toDeserialize = """{
      "class":{"@ref":{"id":"derp","class":{"@ref":{"id":"classes"}}}},
      "data":{"test":{"field1":{"@obj":{"@name":"Test"}}}},
      "ref":{"@ref":{"id":"101727203651223552","class":{"@ref":{"id":"derp","class":{"@ref":{"id":"classes"}}}}}},
      "ts":1433273471399755
    }"""
    val parsed = json.readValue(toDeserialize, classOf[Value])

    parsed should equal (ObjectV(
      "ref" -> RefV("101727203651223552", RefV("derp", Native.Classes)),
      "class" -> RefV("derp", Native.Classes),
      "ts" -> LongV(1433273471399755L),
      "data" -> ObjectV("test" -> ObjectV("field1" -> ObjectV("@name" -> StringV("Test"))))))
  }

  it should "deserialize an invalid reference" in {
    the [JsonMappingException] thrownBy {
      val toDeserialize = """{
        "@ref":{
          "id": "1234567890",
          "class": "it was expected another ref here"
        }
      }"""

      json.readValue(toDeserialize, classOf[Value])
    } should have message "Unexpected value in class field of @ref: StringV(it was expected another ref here)"

    the [JsonMappingException] thrownBy {
      val toDeserialize = """{
        "@ref":{
          "id": "1234567890",
          "database": "it was expected another ref here"
        }
      }"""

      json.readValue(toDeserialize, classOf[Value])
    } should have message "Unexpected value in database field of @ref: StringV(it was expected another ref here)"
  }

  it should "deserialize empty object" in {
    val toDeserialize = """{}"""
    val parsed = json.readValue(toDeserialize, classOf[Value])

    parsed should equal (ObjectV.empty)
  }

  it should "deserialize ts" in {
    val fiveMinutes = Instant.ofEpochMilli(0).plus(5, ChronoUnit.MINUTES)

    val time = json.readValue("""{"@ts":"1970-01-01T00:05:00Z"}""", classOf[Value])
    time should equal (TimeV(fiveMinutes))
    time.to[Instant].get should equal (fiveMinutes)

    val withMillis = json.readValue("""{"@ts":"1970-01-01T00:05:00.001Z"}""", classOf[Value])
    withMillis.to[Instant].get should equal (fiveMinutes.plus(1, ChronoUnit.MILLIS))

    val withMicros = json.readValue("""{"@ts":"1970-01-01T00:05:00.001442Z"}""", classOf[Value]).to[TimeV].get
    withMicros.to[Instant].get should equal (fiveMinutes.plus(1, ChronoUnit.MILLIS).plus(442, ChronoUnit.MICROS))

    val withNanos = json.readValue("""{"@ts":"1970-01-01T00:05:00.001442042Z"}""", classOf[Value])
    withNanos.to[Instant].get should equal (fiveMinutes.plus(1, ChronoUnit.MILLIS).plus(442, ChronoUnit.MICROS).plus(42, ChronoUnit.NANOS))
  }

  it should "deserialize date" in {
    val toDeserialize = """{"@date":"1970-01-03"}"""
    val parsed = json.readValue(toDeserialize, classOf[Value])

    parsed should equal (DateV(LocalDate.ofEpochDay(0).plusDays(2)))
  }

  it should "deserialize bytes" in {
    val bytes = json.readValue("{\"@bytes\":\"AQIDBA==\"}", classOf[Value])

    bytes should equal (BytesV(0x1, 0x2, 0x3, 0x4))
    bytes.to[BytesV].get should equal (BytesV(0x1, 0x2, 0x3, 0x4))
    bytes.to[Array[Byte]].get should equal (Array[Byte](0x1, 0x2, 0x3, 0x4))

    json.readValue("{\"@bytes\":\"-A==\"}", classOf[Value]) should equal (BytesV(0xf8))
    json.readValue("{\"@bytes\":\"-Q==\"}", classOf[Value]) should equal (BytesV(0xf9))
    json.readValue("{\"@bytes\":\"-g==\"}", classOf[Value]) should equal (BytesV(0xfa))
    json.readValue("{\"@bytes\":\"-w==\"}", classOf[Value]) should equal (BytesV(0xfb))
    json.readValue("{\"@bytes\":\"_A==\"}", classOf[Value]) should equal (BytesV(0xfc))
    json.readValue("{\"@bytes\":\"_Q==\"}", classOf[Value]) should equal (BytesV(0xfd))
    json.readValue("{\"@bytes\":\"_g==\"}", classOf[Value]) should equal (BytesV(0xfe))
    json.readValue("{\"@bytes\":\"_w==\"}", classOf[Value]) should equal (BytesV(0xff))
  }
}
