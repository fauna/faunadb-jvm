package faunadb.values

import com.fasterxml.jackson.annotation.JsonProperty

import scala.annotation.meta.{field, param}

/**
 * A Ref.
 *
 * '''Reference''': [[https://faunadb.com/documentation/queries#values-special_types FaunaDB Special Types]]
 */
case class Ref(@(JsonProperty @field @param)("@ref") value: String) extends Value {
  def this(parent: Ref, child: String) = this(parent.value + "/" + child)

  override def asRefOpt: Option[Ref] = Some(this)
}
