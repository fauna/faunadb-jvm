package faunadb

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

object QueryError {
  case class Param(error: String, description: String)
}
case class QueryError(position: Seq[String], code: String, description: String, parameters: Map[String, QueryError.Param])

sealed abstract class FaunaResponse

final case class ErrorResponse(status: Int, error: String) extends FaunaResponse

final case class QueryErrorResponse(status: Int, errors: IndexedSeq[QueryError]) extends FaunaResponse

final class NoContentResponse extends FaunaResponse

final case class ResourceResponse(status: Int, resource: JsonNode, references: ObjectNode) extends FaunaResponse
final case class QueryResponse(status: Int, body: ObjectNode) extends FaunaResponse
