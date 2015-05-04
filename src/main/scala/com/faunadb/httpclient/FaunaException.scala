package com.faunadb.httpclient

import java.util.AbstractMap.SimpleImmutableEntry
import java.util.Optional
import java.util.{Map => JMap}
import java.util.function.{Function => JFunction}

import com.faunadb.query.Error.ValidationFailed
import com.faunadb.query.ValidationError

abstract class FaunaException(message: String) extends Exception(message)

case class UnavailableException(message: String) extends FaunaException("FaunaDB host unavailable: "+message)

class QueryException(val response: QueryErrorResponse) extends FaunaException(response.errors.map(r => r.code + ": " + r.reason).mkString(", ")) {
  def getError[A <: com.faunadb.query.Error](errorType: Class[A]): Optional[A] = response.getError(errorType)
  def getResponse() = response

  def handleError[A <: com.faunadb.query.Error, R](errorType: Class[A], f: JFunction[A, R]): R = {
    response.errors.find { e => errorType.isAssignableFrom(e.getClass) }.map { e => f(e.asInstanceOf[A]) }.getOrElse(throw this)
  }

  def handleValidationError[R](err: ValidationError.Error, f: JFunction[JMap.Entry[String, ValidationError], R]): R = {
    response.errors.find { e => e.isInstanceOf[ValidationFailed] }.flatMap { ve =>
      ve.asInstanceOf[ValidationFailed].parameters.find { case (k,v) => v.error == err.error }
    }.map { case (k,v) =>
      f(new SimpleImmutableEntry[String, ValidationError](k, v))
    }.getOrElse(throw this)
  }
}

case class BadQueryException(r: QueryErrorResponse) extends QueryException(r)
case class NotFoundQueryException(r: QueryErrorResponse) extends QueryException(r)
case class UnknownQueryException(r: QueryErrorResponse) extends QueryException(r)

case class NotFoundException(message: String) extends FaunaException(message)
case class InternalException(message: String) extends FaunaException(message)
case class UnauthorizedException(message: String) extends FaunaException(message)
case class UnknownException(message: String) extends FaunaException(message)
