package com.faunadb.util

import scala.concurrent.Future

object FutureImplicits {
  implicit class RichScalaFuture[A](underlying: Future[A]) {
    def toListenableFuture(): ScalaListenableFuture[A] = {
      new ScalaListenableFuture(underlying)
    }
  }
}
