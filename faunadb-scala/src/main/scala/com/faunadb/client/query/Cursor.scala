package com.faunadb.client.query

import com.fasterxml.jackson.annotation.JsonValue

import scala.annotation.meta.field

/**
 * A FaunaDB cursor, for use when paginating through an ordered set.
 * 
 * Concrete cursors contain a direction: before, or after.
 */
sealed trait Cursor

/**
 * A before cursor.
 */
case class Before(@(JsonValue @field) value: Value) extends Cursor

/**
 * An after cursor.
 */
case class After(@(JsonValue @field) value: Value) extends Cursor

