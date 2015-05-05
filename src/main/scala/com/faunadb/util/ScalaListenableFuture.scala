package com.faunadb.util

import com.google.common.util.concurrent.AbstractFuture

import scala.concurrent.Future
import scala.util.{Failure, Success}

class ScalaListenableFuture[A](val underlying: Future[A]) extends AbstractFuture[A] {
  underlying.onComplete {
    case Success(s) => set(s)
    case Failure(ex) => setException(ex)
  }
}