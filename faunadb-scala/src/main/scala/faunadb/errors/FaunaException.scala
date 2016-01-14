package faunadb.errors

import faunadb.QueryErrorResponse

object FaunaException {
  private[errors] def respToError(response: QueryErrorResponse) = {
    response.errors.map(r => r.code + ": " + r.description).mkString(", ")
  }
}

class FaunaException(response: Option[QueryErrorResponse], msg: String) extends Exception(msg) {
  def this(msg: String) = this(None, msg)
  def this(resp: QueryErrorResponse) = this(Some(resp), FaunaException.respToError(resp))

  def errors = response.get.errors
  def status = response.get.status
}

/**
  * An exception thrown  if FaunaDB responds with an HTTP 503. Such errors represent that
  * the FaunaDB service was unavailable.
  */
case class UnavailableException(response: Option[QueryErrorResponse], message: String) extends FaunaException(response, message) {
  def this(message: String) = this(None, message)
  def this(response: QueryErrorResponse) = this(Some(response), FaunaException.respToError(response))
}

/**
 * An exception thrown if FaunaDB cannot evaluate a query.
 */
class BadRequestException(response: Option[QueryErrorResponse], message: String) extends FaunaException(response, message) {
  def this(message: String) = this(None, message)
  def this(response: QueryErrorResponse) = this(Some(response), FaunaException.respToError(response))
}

/**
 * An exception thrown if FaunaDB responds with an HTTP 404 for non-query endpoints.
 * @param message
 */
class NotFoundException(response: Option[QueryErrorResponse], message: String) extends FaunaException(response, message) {
  def this(message: String) = this(None, message)
  def this(response: QueryErrorResponse) = this(Some(response), FaunaException.respToError(response))
}

/**
 * An exception thrown if FaunaDB responds with an HTTP 500. Such errors represent an internal
 * failure within the database.
 */
class InternalException(response: Option[QueryErrorResponse], message: String) extends FaunaException(response, message) {
  def this(message: String) = this(None, message)
  def this(response: QueryErrorResponse) = this(Some(response), FaunaException.respToError(response))
}

/**
 * An exception thrown if FaunaDB responds with an HTTP 401.
 */
class UnauthorizedException(response: Option[QueryErrorResponse], message: String) extends FaunaException(response, message) {
  def this(message: String) = this(None, message)
  def this(response: QueryErrorResponse) = this(Some(response), FaunaException.respToError(response))
}

class UnknownException(response: Option[QueryErrorResponse], message: String) extends FaunaException(message) {
  def this(message: String) = this(None, message)
  def this(response: QueryErrorResponse) = this(Some(response), FaunaException.respToError(response))
}
