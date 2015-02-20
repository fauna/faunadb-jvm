package com.faunadb.query

import com.fasterxml.jackson.annotation.{JsonValue, JsonProperty}

import scala.annotation.meta.{getter, field}

trait Expression

sealed trait Path
case class ObjectPath(@(JsonValue @getter) field: String) extends Path
case class ArrayPath(@(JsonValue @getter) index: Int) extends Path

case class Let(@(JsonProperty @field)("let") vars: Map[String, Expression], in: Expression) extends Expression
case class Do(@(JsonProperty @field)("do") expressions: Array[Expression]) extends Expression
case class If(@(JsonProperty @field)("if") condition: Expression, then: Expression, `else`: Expression) extends Expression
case class Quote(quote: Expression) extends Expression
case class Fetch(@(JsonProperty @field)("fetch") path: Array[Path], from: Primitive) extends Expression

case class Lambda(@(JsonProperty @field)("lambda") argument: String, expr: Expression)
case class Map(@(JsonProperty @field)("map") lambda: Lambda, collection: Primitive) extends Expression
case class Foreach(@(JsonProperty @field)("foreach") lambda: Lambda, collection: Primitive) extends Expression
