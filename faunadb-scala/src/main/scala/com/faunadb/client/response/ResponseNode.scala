package com.faunadb.client.response

import com.fasterxml.jackson.databind.`type`.TypeFactory
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import com.faunadb.client.query.{Event, Ref}

import scala.collection.AbstractMap

@JsonDeserialize(using=classOf[ResponseNodeDeserializer])
class ResponseNode(private val underlying: JsonNode, json: ObjectMapper) {
  def asStringOpt: Option[String] = Option(underlying.asText())
  def asString: String = asStringOpt.get
  def asNumberOpt: Option[Long] = if (underlying.isNumber) Some(underlying.asLong()) else None
  def asNumber: Long = asNumberOpt.get
  def asDoubleOpt: Option[Double] = if (underlying.isDouble) Some(underlying.asDouble()) else None
  def asDouble: Double = asDoubleOpt.get
  def asArrayOpt: Option[Array[ResponseNode]] = Option(json.convertValue(underlying, TypeFactory.defaultInstance().constructArrayType(classOf[ResponseNode])))
  def asArray: Array[ResponseNode] = asArrayOpt.get
  def asObjectOpt: Option[ResponseMap] = Option(json.convertValue(underlying, classOf[ResponseMap]))
  def asObject = asObjectOpt.get
  def asRefOpt: Option[Ref] = Option(json.convertValue(underlying, classOf[Ref]))
  def asRef: Ref = asRefOpt.get

  def asPageOpt: Option[Page] = Option(json.convertValue(underlying, classOf[Page]))
  def asPage = asPageOpt.get

  def asInstanceOpt: Option[Instance] = Option(json.convertValue(underlying, classOf[Instance]))
  def asInstance = asInstanceOpt.get

  def asKeyOpt: Option[Key] = Option(json.convertValue(underlying, classOf[Key]))
  def asKey = asKeyOpt.get

  def asDatabaseOpt: Option[Database] = Option(json.convertValue(underlying, classOf[Database]))
  def asDatabase = asDatabaseOpt.get

  def asClassOpt: Option[Class] = Option(json.convertValue(underlying, classOf[Class]))
  def asClass = asClassOpt.get

  def asIndexOpt: Option[Index] = Option(json.convertValue(underlying, classOf[Index]))
  def asIndex = asIndexOpt.get

  def asEventOpt: Option[Event] = Option(json.convertValue(underlying, classOf[Event]))
  def asEvent = asEventOpt.get

  override def toString: String = underlying.toString

  override def equals(obj: scala.Any): Boolean = {
    obj.isInstanceOf[ResponseNode] && underlying.equals(obj.asInstanceOf[ResponseNode].underlying)
  }

  override def hashCode(): Int = underlying.hashCode()
}

@JsonDeserialize(using=classOf[ResponseMapDeserializer])
class ResponseMap(underlying: Map[String, ResponseNode]) extends AbstractMap[String, ResponseNode] {
  override def get(key: String): Option[ResponseNode] = underlying.get(key)

  override def +[B1 >: ResponseNode](kv: (String, B1)): collection.Map[String, B1] = underlying + kv

  override def iterator: Iterator[(String, ResponseNode)] = underlying.iterator

  override def -(key: String): collection.Map[String, ResponseNode] = underlying - key

  override def hashCode(): Int = underlying.hashCode()

  override def equals(that: Any): Boolean = underlying.equals(that)
}
