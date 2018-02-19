package com.virtuslab.auctionhouse.primaryasync

import com.google.common.util.concurrent.{FutureCallback, Futures, ListenableFuture}

import scala.concurrent.{Future, Promise}

object AsyncUtils {

  object Implicits {

    implicit class RichListenableFuture[T](lf: ListenableFuture[T]) {
      def asScala: Future[T] = {
        val p = Promise[T]
        Futures.addCallback(lf, new FutureCallback[T] {
          def onFailure(t: Throwable): Unit = p failure t

          def onSuccess(result: T): Unit = p success result
        })
        p.future
      }
    }

  }

}
