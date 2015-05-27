package com.faunadb.client

import java.util.Optional

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.faunadb.client.query.{Ref, Response}

import scala.collection.JavaConverters._

object QueryError {
  case class Param(error: String, reason: String)
}
case class QueryError(position: Seq[String], code: String, reason: String, parameters: Map[String, QueryError.Param]) {
  def getPosition() = position.asJava
  def getCode() = code
  def getReason() = reason
  def getParameters() = parameters.asJava
}

sealed abstract class FaunaResponse

final case class ErrorResponse(status: Int, error: String) extends FaunaResponse

final case class QueryErrorResponse(status: Int, errors: Seq[com.faunadb.client.query.Error]) extends FaunaResponse {
  def getStatus() = status
  def getErrors() = errors.asJava
  def getError[A <: com.faunadb.client.query.Error](errorType: Class[A]): Optional[A] = {
    errors.find(err => errorType.isAssignableFrom(err.getClass))
  }.map(err => Optional.of(err.asInstanceOf[A])).getOrElse(Optional.empty[A]())
}

final class NoContentResponse extends FaunaResponse

final case class ResourceResponse(status: Int, resource: JsonNode, references: ObjectNode) extends FaunaResponse
final case class QueryResponse(status: Int, body: ObjectNode) extends FaunaResponse

case class SetResponse[R <: Response](data: Array[R], before: Option[Ref], after: Option[Ref])
