package com.faunadb.client

abstract class FaunaException(message: String) extends Exception(message)

case class UnavailableException(message: String) extends FaunaException("FaunaDB host unavailable: "+message)

class QueryException(val response: QueryErrorResponse) extends FaunaException(response.errors.map(r => r.code + ": " + r.reason).mkString(", "))

case class BadQueryException(r: QueryErrorResponse) extends QueryException(r)
case class NotFoundQueryException(r: QueryErrorResponse) extends QueryException(r)
case class UnknownQueryException(r: QueryErrorResponse) extends QueryException(r)

case class NotFoundException(message: String) extends FaunaException(message)
case class InternalException(message: String) extends FaunaException(message)
case class UnauthorizedException(message: String) extends FaunaException(message)
case class UnknownException(message: String) extends FaunaException(message)
