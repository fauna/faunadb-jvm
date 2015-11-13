package faunadb.types

import java.time.LocalDate

import com.fasterxml.jackson.annotation.{JsonIgnore, JsonProperty, JsonCreator}

import scala.annotation.meta.{getter, field}

object Date {
  def apply(value: String) = new Date(value)
}

case class Date(@(JsonIgnore @field @getter) value: LocalDate) extends Value {
  @JsonCreator
  def this(@JsonProperty("@date") value: String) = this(LocalDate.parse(value))

  @JsonProperty("@date")
  val strValue = value.toString

  override def asDateOpt = Some(this)
}
