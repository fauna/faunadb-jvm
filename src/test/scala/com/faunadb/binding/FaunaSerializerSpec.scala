package com.faunadb.binding

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.faunadb.FaunaInstance
import com.faunadb.annotation.{FaunaIgnore, FaunaConstraint, FaunaData, FaunaInstanceRef}
import org.scalatest.{Matchers, FlatSpec}
import scala.annotation.meta.field
import scala.collection.JavaConverters._

import scala.beans.BeanInfo

@BeanInfo
case class TestObject(
  @(FaunaInstanceRef @field) var ref: String,
  @(FaunaData @field) var data1: String,
  @(FaunaData @field) var data2: String,
  var data3: Int,
  @(FaunaIgnore @field) var shouldNotSeeMe: Boolean,
  @(FaunaConstraint @field) var constraint1: String) {
  def this() = this("", "", "", 0, true, "")
}

class FaunaBeanSerializerSpec extends FlatSpec with Matchers {
  private val json = new ObjectMapper()

  "Fauna Bean Serializer" should "serialize an annotated bean properly" in {
    val serializer = new BeanSerializer
    val obj = new TestObject("classes/derp/12345", "test", "test2", 3, true, "email1")
    val instance = serializer.serialize(obj)

    val dataMap = json.valueToTree[ObjectNode](Map("data3" -> 3, "data2" -> "test2", "data1" -> "test").asJava)
    val constraintMap = json.valueToTree[ObjectNode](Map("constraint1" -> "email1").asJava)
    instance shouldBe FaunaInstance("classes/derp/12345", "classes/TestObject", 0, dataMap, constraintMap, json.createObjectNode())
  }

  it should "deserialize into an annotated bean properly" in {
    val serializer = new BeanSerializer
    val dataMap = json.valueToTree[ObjectNode](Map("data3" -> 3, "data2" -> "test2", "data1" -> "test").asJava)
    val constraintMap = json.valueToTree[ObjectNode](Map("constraint1" -> "email1").asJava)
    val instance = FaunaInstance("classes/derp/12345", "classes/TestObject", 0, dataMap, constraintMap, json.createObjectNode())
    val obj = serializer.deserialize(classOf[TestObject], instance)

    obj shouldBe new TestObject("classes/derp/12345", "test", "test2", 3, true, "email1")
  }
}
