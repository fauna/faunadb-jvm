package com.faunadb.util

import scala.concurrent.{ExecutionContext, Future}

object FutureImplicits {
  implicit class RichScalaFuture[A](underlying: Future[A])(implicit context: ExecutionContext) {
    def toListenableFuture(): ScalaListenableFuture[A] = {
      new ScalaListenableFuture(underlying)
    }
  }
}
