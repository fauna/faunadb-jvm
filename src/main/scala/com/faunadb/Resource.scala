package com.faunadb

import com.fasterxml.jackson.annotation.{JsonCreator, JsonProperty}
import com.fasterxml.jackson.databind.node.{JsonNodeFactory, ObjectNode}

import scala.collection.mutable

sealed abstract class FaunaResource {  }

case class FaunaInstance(
  @JsonProperty("ref") ref: String = "",
  @JsonProperty("class") classRef: String = "",
  @JsonProperty("ts") ts: Long = 0,
  @JsonProperty("data") data: ObjectNode = JsonNodeFactory.instance.objectNode(),
  @JsonProperty("constraints") constraints: ObjectNode = JsonNodeFactory.instance.objectNode(),
  @JsonProperty("references") references: ObjectNode = JsonNodeFactory.instance.objectNode()) extends FaunaResource {
  def withRef(newRef: String) = copy(ref = newRef)
  def withClass(newClass: String) = copy(classRef = newClass)
  def withTs(newTs: Long) = copy(ts = newTs)
  def withConstraints(newConstraints: ObjectNode) = copy(constraints = newConstraints)
  def build() = new FaunaInstance(ref, classRef, ts, data, constraints)
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
  before: String,
  after: String,
  resources: mutable.LinkedHashMap[String, FaunaInstance]) extends FaunaResource {

  @JsonCreator
  def this(
    @JsonProperty("ref") ref: String,
    @JsonProperty("class") classRef: String,
    @JsonProperty("count") count: Int,
    @JsonProperty("before") before: String,
    @JsonProperty("after") after: String) = this(ref, classRef, count, before, after, new mutable.LinkedHashMap())
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
