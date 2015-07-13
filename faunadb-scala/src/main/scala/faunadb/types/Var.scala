package faunadb.types

import com.fasterxml.jackson.annotation.JsonProperty

import scala.annotation.meta.field

case class Var(@(JsonProperty @field)("var") variable: String) extends Value
