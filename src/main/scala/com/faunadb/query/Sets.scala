package com.faunadb.query

import com.fasterxml.jackson.annotation.JsonProperty

import scala.annotation.meta.field

trait SetDeserialization

sealed trait Set extends Retrievable

case class Match(@(JsonProperty @field)("match") term: String, @(JsonProperty @field) index: Ref) extends Set
case class Union(@(JsonProperty @field)("union") sets: Array[Set]) extends Set
case class Intersection(@(JsonProperty @field)("intersection") sets: Array[Set]) extends Set
case class Difference(@(JsonProperty @field)("difference") sets: Array[Set]) extends Set
case class Join(@(JsonProperty @field)("join") source: Set, @(JsonProperty @field)("with") target: String) extends Set
