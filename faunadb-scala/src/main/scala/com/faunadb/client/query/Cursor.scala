package com.faunadb.client.query

import com.fasterxml.jackson.annotation.JsonValue

import scala.annotation.meta.field

sealed trait Cursor
case class Before(@(JsonValue @field) ref: Value) extends Cursor
case class After(@(JsonValue @field) ref: Value) extends Cursor

