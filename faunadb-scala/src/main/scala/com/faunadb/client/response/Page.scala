package com.faunadb.client.response

/**
 * Represents a FaunaDB Page response. This, like other response types, is obtained by coercing a
 * [[ResponseNode]] using its associated conversion method: [[ResponseNode.asPage]] or [[ResponseNode.asPageOpt]].
 *
 */
case class Page(data: Array[ResponseNode], before: Option[ResponseNode], after: Option[ResponseNode])

