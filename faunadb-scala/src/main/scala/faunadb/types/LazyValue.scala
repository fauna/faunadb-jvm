package faunadb.types

import com.fasterxml.jackson.databind.`type`.TypeFactory
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import faunadb.response._

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
@JsonDeserialize(using=classOf[LazyValueDeserializer])
class LazyValue private[faunadb] (private val underlying: JsonNode, json: ObjectMapper) extends Value {
  /**
   * Coerces the node into a string.
   *
   * @return $some if the coercion is possible, $none if not.
   */
  override def asStringOpt: Option[String] = if (underlying.isTextual) Some(underlying.asText) else None

  /**
   * Coerces the node into a boolean
   *
   * @return $some if the coercion is possible, $none if not.
   */
  override def asBooleanOpt: Option[Boolean] = if (underlying.isBoolean) Some(underlying.asBoolean()) else None

  /**
   * Coerces the node into a long.
   *
   * @return $some if the coercion is possible, $none if not.
   */
  override def asNumberOpt: Option[Long] = if (underlying.isNumber) Some(underlying.asLong()) else None

  /**
   * Coerces the node into a double.
   *
   * @return $some if the coercion is possible, $none if not.
   */
  override def asDoubleOpt: Option[Double] = if (underlying.isDouble) Some(underlying.asDouble()) else None

  /**
   * Coerces the node into an array.
   *
   * @return $some if the coercion is possible, $none if not.
   */
  override def asArrayOpt: Option[Array[Value]] = {
    try {
      Option(json.convertValue(underlying, TypeFactory.defaultInstance().constructArrayType(classOf[LazyValue])))
    } catch {
      case _: IllegalArgumentException => None
    }
  }

  /**
   * Coerces the node into a [[LazyValueMap]].
   *
   * @return $some if the coercion is possible, $none if not.
   */
  override def asObjectOpt: Option[collection.Map[String, Value]] = {
    try {
      Option(json.convertValue(underlying, classOf[LazyValueMap]))
    } catch {
      case _: ClassCastException => None
      case _: IllegalArgumentException => None
    }
  }

  /**
   * Coerces the node into a [[Ref]].
   *
   * @return $some if the coercion is possible, $none if not.
   */
  override def asRefOpt: Option[Ref] = {
    try {
      Option(json.convertValue(underlying, classOf[Ref]))
    } catch {
      case _: IllegalArgumentException => None
    }
  }

  /**
   * Coerces the node into a [[Page]].
   *
   * @return $some if the coercion is possible, $none if not.
   */
  override def asPageOpt: Option[Page] ={
    try {
      Option(json.convertValue(underlying, classOf[Page]))
    } catch {
      case _: IllegalArgumentException => None
    }
  }

  /**
   * Coerces the node into an [[Instance]].
   *
   * @return $some if the coercion is possible, $none if not.
   */
  override def asInstanceOpt: Option[Instance] = {
    try {
      Option(json.convertValue(underlying, classOf[Instance]))
    } catch {
      case _: IllegalArgumentException => None
    }
  }

  /**
   * Coerces the node into a [[Key]].
   *
   * @return $some if the coercion is possible, $none if not.
   */
  override def asKeyOpt: Option[Key] = {
    try {
      Option(json.convertValue(underlying, classOf[Key]))
    } catch {
      case _: IllegalArgumentException => None
    }
  }

  /**
   * Coerces the node into a [[Database]]
   *
   * @return $some if the coercion is possible, $none if not.
   */
  override def asDatabaseOpt: Option[Database] = {
    try {
      Option(json.convertValue(underlying, classOf[Database]))
    } catch {
      case _: IllegalArgumentException => None
    }
  }

  /**
    * Coerces the node into a [[Token]]
    *
    * @return $some if the coercion is possible, $none if not.
    */
  override def asTokenOpt: Option[Token] = {
    try {
      Option(json.convertValue(underlying, classOf[Token]))
    } catch {
      case _: IllegalArgumentException => None
    }
  }

  /**
   * Coerces the node into a [[Class]]
   *
   * @return $some if the coercion is possible, $none if not.
   */
  override def asClassOpt: Option[Class] = {
    try {
      Option(json.convertValue(underlying, classOf[Class]))
    } catch {
      case _: IllegalArgumentException => None
    }
  }

  /**
   * Coerces the node into an [[Index]]
   *
   * @return $some if the coercion is possible, $none if not.
   */
  override def asIndexOpt: Option[Index] = {
    try {
      Option(json.convertValue(underlying, classOf[Index]))
    } catch {
      case _: IllegalArgumentException => None
    }
  }

  /**
   * Coerces the node into an [[Event]].
   *
   * @return $some if the coercion is possible, $none if not.
   */
  override def asEventOpt: Option[Event] = {
    try {
      Option(json.convertValue(underlying, classOf[Event]))
    } catch {
      case _: IllegalArgumentException => None
    }
  }

  /**
   * Coerces the node into a [[Set]].
   *
   * @return $some if the coercion is possible, $none if not.
   */
  override def asSetOpt: Option[Set] = {
    try {
      Option(json.convertValue(underlying, classOf[Set]))
    } catch {
      case _: IllegalArgumentException => None
    }
  }

  override def asTsOpt: Option[Ts] = {
    try {
      Option(json.convertValue(underlying, classOf[Ts]))
    } catch {
      case _: IllegalArgumentException => None
    }
  }

  override def asDateOpt: Option[Date] = {
    try {
      Option(json.convertValue(underlying, classOf[Date]))
    } catch {
      case _: IllegalArgumentException => None
    }
  }

  override def toString: String = underlying.toString

  override def equals(obj: scala.Any): Boolean = {
    obj.isInstanceOf[LazyValue] && underlying.equals(obj.asInstanceOf[LazyValue].underlying)
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
@JsonDeserialize(using=classOf[LazyValueMapDeserializer])
class LazyValueMap private[faunadb] (underlying: Map[String, LazyValue]) extends AbstractMap[String, LazyValue] {
  override def get(key: String): Option[LazyValue] = underlying.get(key)

  override def +[B1 >: LazyValue](kv: (String, B1)): collection.Map[String, B1] = underlying + kv

  override def iterator: Iterator[(String, LazyValue)] = underlying.iterator

  override def -(key: String): collection.Map[String, LazyValue] = underlying - key

  override def hashCode(): Int = underlying.hashCode()

  override def equals(that: Any): Boolean = underlying.equals(that)
}
