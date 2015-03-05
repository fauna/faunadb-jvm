package com.faunadb.httpclient

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

case class ParamError(error: String, reason: String)
case class Error(position: Seq[String], code: String, reason: String, parameters: Map[String, ParamError])

sealed abstract class FaunaResponse
final class NoContentResponse extends FaunaResponse
final case class ErrorResponse(status: Int, errors: Seq[Error]) extends FaunaResponse
final case class ResourceResponse(status: Int, resource: JsonNode, references: ObjectNode) extends FaunaResponse
final case class QueryResponse(status: Int, body: ObjectNode) extends FaunaResponse
