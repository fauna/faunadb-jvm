package faunadb.response

import com.fasterxml.jackson.databind.`type`.TypeFactory
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import faunadb.query.{Event, Ref}

import scala.collection.AbstractMap

/**
 * An abstract node in a FaunaDB response tree. An instance of this class does not have any
 * accessible data. It must first be coerced into a concrete type.
 *
 * @example Consider the ResponseNode modeling the root of the following tree:
 * {{{
 * {
 *   "ref": {"@ref": "some/ref" },
 *   "data": { "someKey": "string1", "someKey2": 123 }
 * }
 * }}}
 *
 * The data in this tree can be accessed using:
 * {{{
 *   node.asObject("ref").asRef // Ref("some/ref")
 *   node.asObject("data").asObject("someKey").asString // "string1"
 * }}}
 *
 * @define some [[scala.Some]]
 * @define none [[scala.None]]
 */
@JsonDeserialize(using=classOf[ResponseNodeDeserializer])
class ResponseNode private[faunadb] (private val underlying: JsonNode, json: ObjectMapper) {
  /**
   * Coerces the node into a string.
   *
   * @return $some if the coercion is possible, $none if not.
   */
  def asStringOpt: Option[String] = if (underlying.isTextual) Some(underlying.asText) else None
  def asString: String = asStringOpt.get

  /**
   * Coerces the node into a boolean
   *
   * @return $some if the coercion is possible, $none if not.
   */
  def asBooleanOpt: Option[Boolean] = if (underlying.isBoolean) Some(underlying.asBoolean()) else None
  def asBoolean: Boolean = asBooleanOpt.get

  /**
   * Coerces the node into a long.
   *
   * @return $some if the coercion is possible, $none if not.
   */
  def asNumberOpt: Option[Long] = if (underlying.isNumber) Some(underlying.asLong()) else None
  def asNumber: Long = asNumberOpt.get

  /**
   * Coerces the node into a double.
   *
   * @return $some if the coercion is possible, $none if not.
   */
  def asDoubleOpt: Option[Double] = if (underlying.isDouble) Some(underlying.asDouble()) else None
  def asDouble: Double = asDoubleOpt.get

  /**
   * Coerces the node into an array.
   *
   * @return $some if the coercion is possible, $none if not.
   */
  def asArrayOpt: Option[Array[ResponseNode]] = {
    try {
      Option(json.convertValue(underlying, TypeFactory.defaultInstance().constructArrayType(classOf[ResponseNode])))
    } catch {
      case _: IllegalArgumentException => None
    }
  }
  def asArray: Array[ResponseNode] = asArrayOpt.get

  /**
   * Coerces the node into a [[ResponseMap]].
   *
   * @return $some if the coercion is possible, $none if not.
   */
  def asObjectOpt: Option[ResponseMap] = {
    try {
      Option(json.convertValue(underlying, classOf[ResponseMap]))
    } catch {
      case _: ClassCastException => None
      case _: IllegalArgumentException => None
    }
  }
  def asObject = asObjectOpt.get

  /**
   * Coerces the node into a [[Ref]].
   *
   * @return $some if the coercion is possible, $none if not.
   */
  def asRefOpt: Option[Ref] = {
    try {
      Option(json.convertValue(underlying, classOf[Ref]))
    } catch {
      case _: IllegalArgumentException => None
    }
  }
  def asRef: Ref = asRefOpt.get

  /**
   * Coerces the node into a [[Page]].
   *
   * @return $some if the coercion is possible, $none if not.
   */
  def asPageOpt: Option[Page] ={
    try {
      Option(json.convertValue(underlying, classOf[Page]))
    } catch {
      case _: IllegalArgumentException => None
    }
  }
  def asPage = asPageOpt.get

  /**
   * Coerces the node into an [[Instance]].
   *
   * @return $some if the coercion is possible, $none if not.
   */
  def asInstanceOpt: Option[Instance] = {
    try {
      Option(json.convertValue(underlying, classOf[Instance]))
    } catch {
      case _: IllegalArgumentException => None
    }
  }
  def asInstance = asInstanceOpt.get

  /**
   * Coerces the node into a [[Key]].
   *
   * @return $some if the coercion is possible, $none if not.
   */
  def asKeyOpt: Option[Key] = {
    try {
      Option(json.convertValue(underlying, classOf[Key]))
    } catch {
      case _: IllegalArgumentException => None
    }
  }
  def asKey = asKeyOpt.get

  /**
   * Coerces the node into a [[Database]]
   *
   * @return $some if the coercion is possible, $none if not.
   */
  def asDatabaseOpt: Option[Database] = {
    try {
      Option(json.convertValue(underlying, classOf[Database]))
    } catch {
      case _: IllegalArgumentException => None
    }
  }
  def asDatabase = asDatabaseOpt.get

  /**
   * Coerces the node into a [[Class]]
   *
   * @return $some if the coercion is possible, $none if not.
   */
  def asClassOpt: Option[Class] = {
    try {
      Option(json.convertValue(underlying, classOf[Class]))
    } catch {
      case _: IllegalArgumentException => None
    }
  }
  def asClass = asClassOpt.get

  /**
   * Coerces the node into an [[Index]]
   *
   * @return $some if the coercion is possible, $none if not.
   */
  def asIndexOpt: Option[Index] = {
    try {
      Option(json.convertValue(underlying, classOf[Index]))
    } catch {
      case _: IllegalArgumentException => None
    }
  }
  def asIndex = asIndexOpt.get

  /**
   * Coerces the node into an [[Event]].
   *
   * @return $some if the coercion is possible, $none if not.
   */
  def asEventOpt: Option[Event] = {
    try {
      Option(json.convertValue(underlying, classOf[Event]))
    } catch {
      case _: IllegalArgumentException => None
    }
  }
  def asEvent = asEventOpt.get

  /**
   * Coerces the node into a [[Set]].
   *
   * @return $some if the coercion is possible, $none if not.
   */
  def asSetOpt: Option[Set] = {
    try {
      Option(json.convertValue(underlying, classOf[Set]))
    } catch {
      case _: IllegalArgumentException => None
    }
  }
  def asSet = asSetOpt.get

  /**
   * Accesses the value of the specified field if this is an object node.
   */
  def apply(key: String): ResponseNode = asObject(key)

  /**
   * Accesses the value of the specified element if this is an array node.
   */
  def apply(index: Int): ResponseNode = asArray(index)

  /**
   * Accesses the value of the specified field if this is an object node.
   */
  def get(key: String): Option[ResponseNode] = asObjectOpt.flatMap(_.get(key))

  /**
   * Accesses the value of the specified element if this is an array node.
   */
  def get(index: Int): Option[ResponseNode] = asArrayOpt.flatMap(_.lift(index))

  override def toString: String = underlying.toString

  override def equals(obj: scala.Any): Boolean = {
    obj.isInstanceOf[ResponseNode] && underlying.equals(obj.asInstanceOf[ResponseNode].underlying)
  }

  override def hashCode(): Int = underlying.hashCode()
}

/**
 * An immutable dictionary of response nodes. FaunaDB responses can be polymorphic, so this
 * dictionary allows individual entries to be coerced into concrete response types as required.
 *
 * ===Example===
 *
 * {{{
 *   val response: ResponseNode // Some result from the FaunaDB client
 *   val responseMap = response.asObject
 *   responseMap("someKey").asString
 *   responseMap("someNumberKey").asNumber
 *
 *   // these can be chained
 *   response.asObject("someObjectKey").asObject("someNestedKey").asString
 * }}}
 */
@JsonDeserialize(using=classOf[ResponseMapDeserializer])
class ResponseMap private[faunadb] (underlying: Map[String, ResponseNode]) extends AbstractMap[String, ResponseNode] {
  override def get(key: String): Option[ResponseNode] = underlying.get(key)

  override def +[B1 >: ResponseNode](kv: (String, B1)): collection.Map[String, B1] = underlying + kv

  override def iterator: Iterator[(String, ResponseNode)] = underlying.iterator

  override def -(key: String): collection.Map[String, ResponseNode] = underlying - key

  override def hashCode(): Int = underlying.hashCode()

  override def equals(that: Any): Boolean = underlying.equals(that)
}
