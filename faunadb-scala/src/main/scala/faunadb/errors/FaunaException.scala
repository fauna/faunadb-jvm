package faunadb.errors

import faunadb.{QueryError, QueryErrorResponse}

object FaunaException {
  private[errors] def respToError(errors: IndexedSeq[QueryError]): String = {
    errors.iterator.map(r => r.code + ": " + r.description).mkString(", ")
  }
}

class FaunaException(response: Option[QueryErrorResponse], msg: String, cause: Throwable = null) extends Exception(msg, cause) {
  def this(msg: String, cause: Throwable) = this(None, msg, cause)
  def this(resp: QueryErrorResponse) = this(Some(resp), FaunaException.respToError(resp.errors), null)

  def errors: IndexedSeq[QueryError] = response.map(_.errors).getOrElse(IndexedSeq.empty)
  def status: Int = response.map(_.status).getOrElse(0)
}

/**
  * An exception thrown  if FaunaDB responds with an HTTP 503. Such errors represent that
  * the FaunaDB service was unavailable.
  */
case class UnavailableException(response: Option[QueryErrorResponse], message: String, cause: Throwable) extends FaunaException(response, message, cause) {
  def this(message: String, cause: Throwable) = this(None, message, cause)
  def this(response: QueryErrorResponse) = this(Some(response), FaunaException.respToError(response.errors), null)
}

/**
 * An exception thrown if FaunaDB cannot evaluate a query.
 */
case class BadRequestException(response: Option[QueryErrorResponse], message: String) extends FaunaException(response, message) {
  def this(message: String) = this(None, message)
  def this(response: QueryErrorResponse) = this(Some(response), FaunaException.respToError(response.errors))
}

/**
 * An exception thrown if FaunaDB responds with an HTTP 404 for non-query endpoints.
 */
case class NotFoundException(response: Option[QueryErrorResponse], message: String) extends FaunaException(response, message) {
  def this(message: String) = this(None, message)
  def this(response: QueryErrorResponse) = this(Some(response), FaunaException.respToError(response.errors))
}

/**
  * An exception thrown if FaunaDB responds with an HTTP 429, meaning the request was throttled.
  */
case class TooManyRequestsException(response: Option[QueryErrorResponse], message: String) extends FaunaException(response, message) {
  def this(message: String) = this(None, message)
  def this(response: QueryErrorResponse) = this(Some(response), FaunaException.respToError(response.errors))
}

/**
  * Exception thrown if FaunaDB responds with an HTTP 410.
  * One example of this is if an account is disabled.
  */
case class ResourceNotAvailableException(response: Option[QueryErrorResponse], message: String) extends FaunaException(response, message) {
  def this(message: String) = this(None, message)
  def this(response: QueryErrorResponse) = this(Some(response), FaunaException.respToError(response.errors))
}

/**
 * An exception thrown if FaunaDB responds with an HTTP 500. Such errors represent an internal
 * failure within the database.
 */
case class InternalException(response: Option[QueryErrorResponse], message: String) extends FaunaException(response, message) {
  def this(message: String) = this(None, message)
  def this(response: QueryErrorResponse) = this(Some(response), FaunaException.respToError(response.errors))
}

/**
 * An exception thrown if FaunaDB responds with an HTTP 401.
 */
case class UnauthorizedException(response: Option[QueryErrorResponse], message: String) extends FaunaException(response, message) {
  def this(message: String) = this(None, message)
  def this(response: QueryErrorResponse) = this(Some(response), FaunaException.respToError(response.errors))
}

/**
  * An exception thrown if FaunaDB responds with an HTTP 403.
  */
case class PermissionDeniedException(response: Option[QueryErrorResponse], message: String) extends FaunaException(response, message) {
  def this(message: String) = this(None, message)
  def this(response: QueryErrorResponse) = this(Some(response), FaunaException.respToError(response.errors))
}

/**
  * An exception thrown if FaunaDB responds with an HTTP 409.
  */
case class TransactionContentionException(response: Option[QueryErrorResponse], message: String) extends FaunaException(response, message) {
  def this(message: String) = this(None, message)
  def this(response: QueryErrorResponse) = this(Some(response), FaunaException.respToError(response.errors))
}

case class UnknownException(response: Option[QueryErrorResponse], message: String, cause: Throwable) extends FaunaException(response, message, cause) {
  def this(message: String, cause: Throwable) = this(None, message, cause)
  def this(response: QueryErrorResponse) = this(Some(response), FaunaException.respToError(response.errors), null)
}

case class StreamingException(message: String) extends FaunaException(None, message) {
  def this(queryError: QueryError) = this(s"Stream interrupted by error event:\n${FaunaException.respToError(Vector(queryError))}")
}