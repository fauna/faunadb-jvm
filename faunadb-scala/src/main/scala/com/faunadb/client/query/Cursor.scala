package com.faunadb.client.query

import com.fasterxml.jackson.annotation.JsonValue

import scala.annotation.meta.field

sealed trait Cursor
case class Before(@(JsonValue @field) ref: Ref) extends Cursor
case class After(@(JsonValue @field) ref: Ref) extends Cursor

