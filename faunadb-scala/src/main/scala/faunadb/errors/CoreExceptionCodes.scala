package faunadb.errors

import enumeratum._
import scala.collection.immutable

sealed abstract class CoreExceptionCodes(override val entryName: String) extends EnumEntry

object CoreExceptionCodes extends Enum[CoreExceptionCodes] {
  val values: immutable.IndexedSeq[CoreExceptionCodes] = findValues
  case object INVALID_ARGUMENT extends CoreExceptionCodes("invalid argument")
  case object CALL_ERROR extends CoreExceptionCodes("call error")
  case object PERMISSION_DENIED extends CoreExceptionCodes("permission denied")
  case object INVALID_EXPRESSION extends CoreExceptionCodes("invalid expression")
  case object INVALID_URL_PARAMETER extends CoreExceptionCodes("invalid url parameter")
  case object SCHEMA_NOT_FOUND extends CoreExceptionCodes("schema not found")
  case object TRANSACTION_ABORTED extends CoreExceptionCodes("transaction aborted")
  case object INVALID_WRITE_TIME extends CoreExceptionCodes("invalid write time")
  case object INVALID_REF extends CoreExceptionCodes("invalid ref")
  case object MISSING_IDENTITY extends CoreExceptionCodes("missing identity")
  case object INVALID_SCOPE extends CoreExceptionCodes("invalid scope")
  case object INVALID_TOKEN extends CoreExceptionCodes("invalid token")
  case object STACK_OVERFLOW extends CoreExceptionCodes("stack overflow")
  case object AUTHENTICATION_FAILED extends CoreExceptionCodes("authentication failed")
  case object VALUE_NOT_FOUND extends CoreExceptionCodes("value not found")
  case object INSTANCE_NOT_FOUND extends CoreExceptionCodes("instance not found")
  case object INSTANCE_ALREADY_EXISTS extends CoreExceptionCodes("instance already exists")
  case object VALIDATION_FAILED extends CoreExceptionCodes("validation failed")
  case object INSTANCE_NOT_UNIQUE extends CoreExceptionCodes("instance not unique")
  case object INVALID_OBJECT_IN_CONTAINER extends CoreExceptionCodes("invalid object in container")
  case object MOVE_DATABASE_ERROR extends CoreExceptionCodes("move database error")
  case object RECOVERY_FAILED extends CoreExceptionCodes("recovery failed")
  case object FEATURE_NOT_AVAILABLE extends CoreExceptionCodes("feature not available")
  case object UNKNOWN_ERROR extends CoreExceptionCodes("unknown error")
}

//object Test{
//  val t: CoreExceptionCodes = CoreExceptionCodes.withName("invalid argument")
//  val g: immutable.Seq[CoreExceptionCodes] = CoreExceptionCodes.values
//}

