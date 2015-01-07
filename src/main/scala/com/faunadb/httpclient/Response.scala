package com.faunadb.httpclient

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode

sealed abstract class FaunaResponse
final class NoContentResponse extends FaunaResponse
final case class ErrorResponse(status: Int, error: String) extends FaunaResponse
final case class ResourceResponse(status: Int, resource: JsonNode, references: ObjectNode) extends FaunaResponse
