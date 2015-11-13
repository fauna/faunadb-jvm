package faunadb.types

import java.time.format.DateTimeFormatter
import java.time.{ZonedDateTime, Instant}

import com.fasterxml.jackson.annotation.{JsonIgnore, JsonCreator, JsonProperty}

import scala.annotation.meta.{getter, param, field}

object Ts {
  def apply(value: String) = new Ts(value)
}

case class Ts(@(JsonIgnore @field @getter) value: Instant) extends Value {
  @JsonCreator
  def this(@JsonProperty("@ts") value: String) = this(ZonedDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant)

  @JsonProperty("@ts")
  val strValue = value.toString

  override def asTsOpt = Some(this)
}
