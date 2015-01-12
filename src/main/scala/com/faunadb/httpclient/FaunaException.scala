package com.faunadb.httpclient

object FaunaException {
  def wrapResponse(response: ErrorResponse) = {
    response.status match {
      case 400 => BadRequestException(response)
      case 401 => UnauthorizedException(response)
      case 404 => NotFoundException(response)
      case 500 => InternalException(response)
      case _ => UnknownException(response)
    }
  }
}

abstract class FaunaException(response: ErrorResponse) extends Exception(response.error)
case class UnauthorizedException(response: ErrorResponse) extends FaunaException(response)
case class BadRequestException(response: ErrorResponse) extends FaunaException(response)
case class NotFoundException(response: ErrorResponse) extends FaunaException(response)
case class InternalException(response: ErrorResponse) extends FaunaException(response)
case class UnknownException(response: ErrorResponse) extends FaunaException(response)
