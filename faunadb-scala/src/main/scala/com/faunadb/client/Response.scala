package com.faunadb.client

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.faunadb.client.query.{Ref, Response}

object QueryError {
  case class Param(error: String, reason: String)
}
case class QueryError(position: Seq[String], code: String, reason: String, parameters: Map[String, QueryError.Param])

sealed abstract class FaunaResponse

final case class ErrorResponse(status: Int, error: String) extends FaunaResponse

final case class QueryErrorResponse(status: Int, errors: Seq[com.faunadb.client.query.Error]) extends FaunaResponse

final class NoContentResponse extends FaunaResponse

final case class ResourceResponse(status: Int, resource: JsonNode, references: ObjectNode) extends FaunaResponse
final case class QueryResponse(status: Int, body: ObjectNode) extends FaunaResponse

case class SetResponse[R <: Response](data: Array[R], before: Option[Ref], after: Option[Ref])
