package com.faunadb

import com.fasterxml.jackson.annotation.{JsonCreator, JsonProperty}
import com.fasterxml.jackson.databind.node.{JsonNodeFactory, ObjectNode}

import scala.collection.mutable

sealed abstract class FaunaResource {  }

case class FaunaInstance(
  @JsonProperty("ref") ref: String = "",
  @JsonProperty("class") classRef: String = "",
  @JsonProperty("ts") ts: Long = 0,
  @JsonProperty("data") data: ObjectNode = JsonNodeFactory.instance.objectNode()
) extends FaunaResource {
  def withRef(newRef: String) = copy(ref = newRef)
  def withClass(newClass: String) = copy(classRef = newClass)
  def withTs(newTs: Long) = copy(ts = newTs)

  def getRef() = ref
  def getClassRef() = classRef
  def getTs() = ts
  def getData() = data
}

case class FaunaDatabase(
  @JsonProperty("ref") ref: String,
  @JsonProperty("class") classRef: String,
  @JsonProperty("name") name: String,
  @JsonProperty("ts") ts: Long) extends FaunaResource

case class FaunaSet(
  ref: String,
  classRef: String,
  count: Int,
  before: Option[String],
  after: Option[String],
  resources: mutable.LinkedHashMap[String, FaunaInstance]) extends FaunaResource {

  @JsonCreator
  def this(
    @JsonProperty("ref") ref: String,
    @JsonProperty("class") classRef: String,
    @JsonProperty("count") count: Int,
    @JsonProperty("before") before: Option[String] = None,
    @JsonProperty("after") after: Option[String] = None) = this(ref, classRef, count, before, after, new mutable.LinkedHashMap())

  def getRef() = ref
  def getClassRef() = classRef
  def getCount() = count
  def getBefore() = before
  def getAfter() = after
  def getResources() = resources
}

case class FaunaKey(
  @JsonProperty("ref") ref: String,
  @JsonProperty("class") classRef: String,
  @JsonProperty("database") dbRef: String,
  @JsonProperty("ts") ts: Long,
  @JsonProperty("role") role: String,
  @JsonProperty("secret") secret: String,
  @JsonProperty("hashed_secret") hashedSecret: String) extends FaunaResource

case class FaunaClass(
  @JsonProperty("ref") ref: String,
  @JsonProperty("class") classRef: String,
  @JsonProperty("name") name: String,
  @JsonProperty("ts") ts: Long,
  @JsonProperty("history_days") historyDays: Int)

case class FaunaIndex(
  @JsonProperty("ref") ref: String,
  @JsonProperty("name") name: String,
  @JsonProperty("class") classRef: String,
  @JsonProperty("source") sourceRef: String,
  @JsonProperty("path") path: String,
  @JsonProperty("unique") unique: Boolean,
  @JsonProperty("active") active: Boolean,
  @JsonProperty("ts") ts: Long)
