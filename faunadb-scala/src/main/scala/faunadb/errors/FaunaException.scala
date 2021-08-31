package faunadb.errors

import faunadb.QueryError

case class InvalidArgumentException(message: String, statusCode: Int, position: Seq[String]) extends FaunaException(message, statusCode, position){
  override val code: CoreExceptionCodes = CoreExceptionCodes.INVALID_ARGUMENT
}
case class FunctionCallException(message: String, statusCode: Int, position: Seq[String], faunaException: List[FaunaException]) extends FaunaException(message, statusCode, position) {
  override val code: CoreExceptionCodes = CoreExceptionCodes.CALL_ERROR
}
case class PermissionDeniedException(message: String, statusCode: Int, position: Seq[String]) extends FaunaException(message, statusCode, position) {
  override val code: CoreExceptionCodes = CoreExceptionCodes.PERMISSION_DENIED
}
case class InvalidExpressionException(message: String, statusCode: Int, position: Seq[String]) extends FaunaException(message, statusCode, position) {
  override val code: CoreExceptionCodes = CoreExceptionCodes.INVALID_EXPRESSION
}
case class InvalidUrlParameterException(message: String, statusCode: Int, position: Seq[String]) extends FaunaException(message, statusCode, position) {
  override val code: CoreExceptionCodes = CoreExceptionCodes.INVALID_URL_PARAMETER
}
case class SchemaNotFoundException(message: String, statusCode: Int, position: Seq[String]) extends FaunaException(message, statusCode, position) {
  override val code: CoreExceptionCodes = CoreExceptionCodes.SCHEMA_NOT_FOUND
}
case class TransactionAbortedException(message: String, statusCode: Int, position: Seq[String]) extends FaunaException(message, statusCode, position) {
  override val code: CoreExceptionCodes = CoreExceptionCodes.TRANSACTION_ABORTED
}
case class InvalidWriteTimeException(message: String, statusCode: Int, position: Seq[String]) extends FaunaException(message, statusCode, position) {
  override val code: CoreExceptionCodes = CoreExceptionCodes.INVALID_WRITE_TIME
}
case class InvalidReferenceException(message: String, statusCode: Int, position: Seq[String]) extends FaunaException(message, statusCode, position) {
  override val code: CoreExceptionCodes = CoreExceptionCodes.INVALID_REF
}
case class MissingIdentityException(message: String, statusCode: Int, position: Seq[String]) extends FaunaException(message, statusCode, position) {
  override val code: CoreExceptionCodes = CoreExceptionCodes.MISSING_IDENTITY
}
case class InvalidScopeException(message: String, statusCode: Int, position: Seq[String]) extends FaunaException(message, statusCode, position) {
  override val code: CoreExceptionCodes = CoreExceptionCodes.INVALID_SCOPE
}
case class InvalidTokenException(message: String, statusCode: Int, position: Seq[String]) extends FaunaException(message, statusCode, position) {
  override val code: CoreExceptionCodes = CoreExceptionCodes.INVALID_TOKEN
}
case class StackOverflowException(message: String, statusCode: Int, position: Seq[String]) extends FaunaException(message, statusCode, position) {
  override val code: CoreExceptionCodes = CoreExceptionCodes.STACK_OVERFLOW
}
case class AuthenticationFailedException(message: String, statusCode: Int, position: Seq[String]) extends FaunaException(message, statusCode, position) {
  override val code: CoreExceptionCodes = CoreExceptionCodes.AUTHENTICATION_FAILED
}
case class ValueNotFoundException(message: String, statusCode: Int, position: Seq[String]) extends FaunaException(message, statusCode, position) {
  override val code: CoreExceptionCodes = CoreExceptionCodes.VALUE_NOT_FOUND
}
case class InstanceNotFoundException(message: String, statusCode: Int, position: Seq[String]) extends FaunaException(message, statusCode, position) {
  override val code: CoreExceptionCodes = CoreExceptionCodes.INSTANCE_NOT_FOUND
}
case class InstanceAlreadyExistsException(message: String, statusCode: Int, position: Seq[String]) extends FaunaException(message, statusCode, position) {
  override val code: CoreExceptionCodes = CoreExceptionCodes.INSTANCE_ALREADY_EXISTS
}
case class InstanceNotUniqueException(message: String, statusCode: Int, position: Seq[String]) extends FaunaException(message, statusCode, position) {
  override val code: CoreExceptionCodes = CoreExceptionCodes.INSTANCE_NOT_UNIQUE
}
case class InvalidObjectInContainerException(message: String, statusCode: Int, position: Seq[String]) extends FaunaException(message, statusCode, position) {
  override val code: CoreExceptionCodes = CoreExceptionCodes.INVALID_OBJECT_IN_CONTAINER
}
case class MoveDatabaseException(message: String, statusCode: Int, position: Seq[String]) extends FaunaException(message, statusCode, position) {
  override val code: CoreExceptionCodes = CoreExceptionCodes.MOVE_DATABASE_ERROR
}
case class RecoveryFailedException(message: String, statusCode: Int, position: Seq[String]) extends FaunaException(message, statusCode, position) {
  override val code: CoreExceptionCodes = CoreExceptionCodes.RECOVERY_FAILED
}
case class FeatureNotAvailableException(message: String, statusCode: Int, position: Seq[String]) extends FaunaException(message, statusCode, position) {
  override val code: CoreExceptionCodes = CoreExceptionCodes.FEATURE_NOT_AVAILABLE
}
case class ProcessingTimeLimitExceededException(m: String, sC: Int) extends FaunaException(m, sC, Seq.empty)

case class ValidationFailedException(m: String, sC: Int, pos: Seq[String], f: Seq[String]) extends FaunaException(m, sC, pos, null, f) {
  override val code: CoreExceptionCodes = CoreExceptionCodes.VALIDATION_FAILED
}

case class UnavailableException(message: String, statusCode: Int) extends FaunaException(message, statusCode, Seq.empty) {
  def this(message: String, cause: Throwable) = this(message, 0)
}
case class BadRequestException(message: String, statusCode: Int) extends FaunaException(message, statusCode, Seq.empty, null)
case class NotFoundException(message: String) extends FaunaException(message, null)
case class InternalException(message: String, statusCode: Int) extends FaunaException(message, null)

object FaunaException {
  private[errors] def respToError(errors: IndexedSeq[QueryError]): String = {
    errors.iterator.map(r => r.code + ": " + r.description).mkString(", ")
  }
}

class FaunaException(msg: String, httpStatusCode: Int, pos: Seq[String], cause: Throwable = null, failures: Seq[String] = Seq.empty)
  extends Exception(msg, cause) {
  def this(msg: String, cause: Throwable) = this(msg, 0, Seq.empty, cause);

  def status: Int = httpStatusCode
  def code(): CoreExceptionCodes = null
}

case class UnauthorizedException(message: String, statusCode: Int) extends FaunaException(message, statusCode, Seq.empty)
case class BadGatewayException(message: String, statusCode: Int) extends FaunaException(message, statusCode, Seq.empty)

case class UnknownException(message: String, cause: Throwable = null) extends FaunaException(message, cause)

case class StreamingException(message: String) extends FaunaException(message, null) {
  def this(queryError: QueryError) = this(s"Stream interrupted by error event:\n${FaunaException.respToError(Vector(queryError))}")
}
