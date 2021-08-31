package faunadb.errors

import faunadb.QueryError

class InvalidArgumentException(m: String, sC: Int, pos: Seq[String]) extends FaunaException(m, sC, pos){
  override val code: CoreExceptionCodes = CoreExceptionCodes.INVALID_ARGUMENT
}
class FunctionCallException(m: String, sC: Int, pos: Seq[String]) extends FaunaException(m, sC, pos) {
  override val code: CoreExceptionCodes = CoreExceptionCodes.CALL_ERROR
}
case class PermissionDeniedException(m: String, sC: Int, pos: Seq[String]) extends FaunaException(m, sC, pos) {
  override val code: CoreExceptionCodes = CoreExceptionCodes.PERMISSION_DENIED
}
case class InvalidExpressionException(m: String, sC: Int, pos: Seq[String]) extends FaunaException(m, sC, pos) {
  override val code: CoreExceptionCodes = CoreExceptionCodes.INVALID_EXPRESSION
}
case class InvalidUrlParameterException(m: String, sC: Int, pos: Seq[String]) extends FaunaException(m, sC, pos) {
  override val code: CoreExceptionCodes = CoreExceptionCodes.INVALID_URL_PARAMETER
}
case class SchemaNotFoundException(m: String, sC: Int, pos: Seq[String]) extends FaunaException(m, sC, pos) {
  override val code: CoreExceptionCodes = CoreExceptionCodes.SCHEMA_NOT_FOUND
}
case class TransactionAbortedException(m: String, sC: Int, pos: Seq[String]) extends FaunaException(m, sC, pos) {
  override val code: CoreExceptionCodes = CoreExceptionCodes.TRANSACTION_ABORTED
}
case class InvalidWriteTimeException(m: String, sC: Int, pos: Seq[String]) extends FaunaException(m, sC, pos) {
  override val code: CoreExceptionCodes = CoreExceptionCodes.INVALID_WRITE_TIME
}
case class InvalidReferenceException(m: String, sC: Int, pos: Seq[String]) extends FaunaException(m, sC, pos) {
  override val code: CoreExceptionCodes = CoreExceptionCodes.INVALID_REF
}
case class MissingIdentityException(m: String, sC: Int, pos: Seq[String]) extends FaunaException(m, sC, pos) {
  override val code: CoreExceptionCodes = CoreExceptionCodes.MISSING_IDENTITY
}
case class InvalidScopeException(m: String, sC: Int, pos: Seq[String]) extends FaunaException(m, sC, pos) {
  override val code: CoreExceptionCodes = CoreExceptionCodes.INVALID_SCOPE
}
case class InvalidTokenException(m: String, sC: Int, pos: Seq[String]) extends FaunaException(m, sC, pos) {
  override val code: CoreExceptionCodes = CoreExceptionCodes.INVALID_TOKEN
}
case class StackOverflowException(m: String, sC: Int, pos: Seq[String]) extends FaunaException(m, sC, pos) {
  override val code: CoreExceptionCodes = CoreExceptionCodes.STACK_OVERFLOW
}
case class AuthenticationFailedException(m: String, sC: Int, pos: Seq[String]) extends FaunaException(m, sC, pos) {
  override val code: CoreExceptionCodes = CoreExceptionCodes.AUTHENTICATION_FAILED
}
case class ValueNotFoundException(m: String, sC: Int, pos: Seq[String]) extends FaunaException(m, sC, pos) {
  override val code: CoreExceptionCodes = CoreExceptionCodes.VALUE_NOT_FOUND
}
case class InstanceNotFoundException(m: String, sC: Int, pos: Seq[String]) extends FaunaException(m, sC, pos) {
  override val code: CoreExceptionCodes = CoreExceptionCodes.INSTANCE_NOT_FOUND
}
case class InstanceAlreadyExistsException(m: String, sC: Int, pos: Seq[String]) extends FaunaException(m, sC, pos) {
  override val code: CoreExceptionCodes = CoreExceptionCodes.INSTANCE_ALREADY_EXISTS
}
case class InstanceNotUniqueException(m: String, sC: Int, pos: Seq[String]) extends FaunaException(m, sC, pos) {
  override val code: CoreExceptionCodes = CoreExceptionCodes.INSTANCE_NOT_UNIQUE
}
case class InvalidObjectInContainerException(m: String, sC: Int, pos: Seq[String]) extends FaunaException(m, sC, pos) {
  override val code: CoreExceptionCodes = CoreExceptionCodes.INVALID_OBJECT_IN_CONTAINER
}
case class MoveDatabaseException(m: String, sC: Int, pos: Seq[String]) extends FaunaException(m, sC, pos) {
  override val code: CoreExceptionCodes = CoreExceptionCodes.MOVE_DATABASE_ERROR
}
case class RecoveryFailedException(m: String, sC: Int, pos: Seq[String]) extends FaunaException(m, sC, pos) {
  override val code: CoreExceptionCodes = CoreExceptionCodes.RECOVERY_FAILED
}
case class FeatureNotAvailableException(m: String, sC: Int, pos: Seq[String]) extends FaunaException(m, sC, pos) {
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
