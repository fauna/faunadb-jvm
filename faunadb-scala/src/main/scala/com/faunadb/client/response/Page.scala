package com.faunadb.client.response

import com.faunadb.client.query.Ref

case class Page(data: Array[ResponseNode], before: Option[Ref], after: Option[Ref])

