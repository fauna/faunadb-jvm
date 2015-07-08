package faunadb.util

import com.google.common.util.concurrent.{FutureCallback, Futures, ListenableFuture}

import scala.concurrent.{Future, Promise}

private[faunadb] object FutureImplicits {
  implicit class RichListenableFuture[A](underlying: ListenableFuture[A]) {
    def asScalaFuture(): Future[A] = {
      val promise = Promise[A]()
      Futures.addCallback(underlying, new FutureCallback[A] {
        override def onFailure(throwable: Throwable): Unit =  promise.failure(throwable)
        override def onSuccess(v: A): Unit = promise.success(v)
      })
      promise.future
    }
  }
}
