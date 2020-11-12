package faunadb.streaming

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{Flow, SubmissionPublisher}

import faunadb.values._
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

private [faunadb] class SnapshotEventFlowProcessor(loadDocument: () => Future[Value])(implicit ec: ExecutionContext)
  extends SubmissionPublisher[Value] with Flow.Processor[Value, Value] {
  private val log = LoggerFactory.getLogger(getClass)
  private var subscription: Flow.Subscription = _
  private var subscriber: Flow.Subscriber[_ >: Value] = _
  private val initialized = new AtomicBoolean(false)
  @volatile private var snapshotTS: Long = _

  // We do not request data from the publisher until we have one subscriber
  // to avoid discarding events before the subscriber had the chance to subscribe.
  override def subscribe(subscriber: Flow.Subscriber[_ >: Value]): Unit = {
    if (this.subscriber == null) {
      this.subscriber = subscriber
      super.subscribe(subscriber)
      requestOne()
    } else
      throw new IllegalStateException("SnapshotEventFlowProcessor can have only one subscriber")
  }

  override def onSubscribe(subscription: Flow.Subscription): Unit =
    this.subscription = subscription

  override def onNext(event: Value): Unit = {
    if (initialized.get()) {
      val eventTS = event("txn").to[Long].get
      if (eventTS > snapshotTS) submit(event) // ignore event older than doc. snapshot
      requestOne()
    } else {
      // not initialized receiving first element
      event("type") match {
        case VSuccess(StringV("start"), _) =>
          loadDocument().onComplete {
            case Failure(exception) =>
              onError(exception)
              subscription.cancel()
            case Success(documentSnapshot) =>
              snapshotTS = documentSnapshot("ts").to[Long].get
              // send start event first
              submit(event)
              // follow up with the snapshot event
              val documentEvent = ObjectV(
                "type" -> StringV("snapshot"),
                "txn" -> LongV(snapshotTS.toLong),
                "event" -> documentSnapshot)
              submit(documentEvent)
              initialized.set(true)
              // only request more when we are ready in order to avoid race condition
              requestOne()
          }
        case _ =>
          onError(new IllegalArgumentException(s"Stream did not begin with a `start` event but $event"))
          subscription.cancel()
      }
    }
  }

  override def onError(throwable: Throwable): Unit = {
    log.error("unrecoverable error encountered by subscription", throwable)
    subscriber.onError(throwable)
  }

  override def onComplete(): Unit = {
    log.debug("subscription completed")
    subscriber.onComplete()
  }

  private def requestOne(): Unit =
    subscription.request(1)
}

