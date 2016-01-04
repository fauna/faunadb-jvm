package faunadb.types

import com.fasterxml.jackson.databind.`type`.TypeFactory
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import faunadb.response._
import faunadb.query._

import scala.collection.AbstractMap

/**
  * A [[Value]] that wraps a JSON response tree. This Value does not convert into a concrete
  * type until one of the type coercion methods is called.
  */
@JsonDeserialize(using=classOf[LazyValueDeserializer])
class LazyValue private[faunadb] (private val underlying: JsonNode, json: ObjectMapper) extends Value {
  def asBefore = Before(RawV(underlying))
  def asAfter = After(RawV(underlying))

  override def asStringOpt: Option[String] = if (underlying.isTextual) Some(underlying.asText) else None
  override def asBooleanOpt: Option[Boolean] = if (underlying.isBoolean) Some(underlying.asBoolean()) else None
  override def asNumberOpt: Option[Long] = if (underlying.isNumber) Some(underlying.asLong()) else None
  override def asDoubleOpt: Option[Double] = if (underlying.isDouble) Some(underlying.asDouble()) else None

  override def asArrayOpt: Option[Array[Value]] = {
    try {
      Option(json.convertValue(underlying, TypeFactory.defaultInstance().constructArrayType(classOf[LazyValue])))
    } catch {
      case _: IllegalArgumentException => None
    }
  }
  override def asObjectOpt: Option[collection.Map[String, Value]] = {
    try {
      Option(json.convertValue(underlying, classOf[LazyValueMap]))
    } catch {
      case _: ClassCastException => None
      case _: IllegalArgumentException => None
    }
  }

  override def asRefOpt: Option[Ref] = {
    try {
      Option(json.convertValue(underlying, classOf[Ref]))
    } catch {
      case _: IllegalArgumentException => None
    }
  }

  override def asPageOpt: Option[Page] ={
    try {
      Option(json.convertValue(underlying, classOf[Page]))
    } catch {
      case _: IllegalArgumentException => None
    }
  }

  override def asInstanceOpt: Option[Instance] = {
    try {
      Option(json.convertValue(underlying, classOf[Instance]))
    } catch {
      case _: IllegalArgumentException => None
    }
  }

  override def asKeyOpt: Option[Key] = {
    try {
      Option(json.convertValue(underlying, classOf[Key]))
    } catch {
      case _: IllegalArgumentException => None
    }
  }

  override def asDatabaseOpt: Option[Database] = {
    try {
      Option(json.convertValue(underlying, classOf[Database]))
    } catch {
      case _: IllegalArgumentException => None
    }
  }

  override def asTokenOpt: Option[Token] = {
    try {
      Option(json.convertValue(underlying, classOf[Token]))
    } catch {
      case _: IllegalArgumentException => None
    }
  }

  override def asClassOpt: Option[Class] = {
    try {
      Option(json.convertValue(underlying, classOf[Class]))
    } catch {
      case _: IllegalArgumentException => None
    }
  }

  override def asIndexOpt: Option[Index] = {
    try {
      Option(json.convertValue(underlying, classOf[Index]))
    } catch {
      case _: IllegalArgumentException => None
    }
  }

  override def asEventOpt: Option[Event] = {
    try {
      Option(json.convertValue(underlying, classOf[Event]))
    } catch {
      case _: IllegalArgumentException => None
    }
  }

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
      case ex: IllegalArgumentException =>
        println(underlying)
        println(ex)
        None
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
