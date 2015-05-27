package com.faunadb.client.util

import com.google.common.util.concurrent.AbstractFuture

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

class ScalaListenableFuture[A](val underlying: Future[A])(implicit context: ExecutionContext) extends AbstractFuture[A] {
  def getUnderlying() = underlying

  underlying.onComplete {
    case Success(s) => set(s)
    case Failure(ex) => setException(ex)
  }
}