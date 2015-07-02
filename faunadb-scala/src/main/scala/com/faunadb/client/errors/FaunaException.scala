package com.faunadb.client.errors

import com.faunadb.client.QueryErrorResponse

abstract class FaunaException(message: String) extends Exception(message)

case class UnavailableException(message: String) extends FaunaException("FaunaDB host unavailable: "+message)

/**
 * A general exception in evaluating or executing a FaunaDB query.
 */
class QueryException(val response: QueryErrorResponse) extends FaunaException(response.errors.map(r => r.code + ": " + r.reason).mkString(", ")) {
  val errors = response.errors
  val status = response.status
}

/**
 * An exception thrown in FaunaDB cannot evaluate a query.
 */
class BadQueryException(r: QueryErrorResponse) extends QueryException(r)

/**
 * An exception thrown in FaunaDB responds with an HTTP 404 when executing a query.
 */
class NotFoundQueryException(r: QueryErrorResponse) extends QueryException(r)

/**
 * An exception thrown if the query error is unknown, or if it is unparseable.
 */
class UnknownQueryException(r: QueryErrorResponse) extends QueryException(r)

/**
 * An exception thrown if FaunaDB responds with an HTTP 404 for non-query endpoints.
 * @param message
 */
case class NotFoundException(message: String) extends FaunaException(message)

/**
 * An exception thrown if FaunaDB responds with an HTTP 500. Such errors represent an internal
 * failure within the database.
 */
case class InternalException(message: String) extends FaunaException(message)

/**
 * An exception thrown if FaunaDB responds with an HTTP 401.
 */
case class UnauthorizedException(message: String) extends FaunaException(message)

case class UnknownException(message: String) extends FaunaException(message)
