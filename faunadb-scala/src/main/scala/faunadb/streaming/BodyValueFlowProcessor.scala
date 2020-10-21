package faunadb.streaming

import java.nio.ByteBuffer
import java.util
import java.util.concurrent.{Flow, SubmissionPublisher}

import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import faunadb.QueryError
import faunadb.errors.{StreamingException, UnknownException}
import faunadb.values.{StringV, VSuccess, Value}
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters.asScalaIteratorConverter
import scala.util.Try

private [faunadb] class BodyValueFlowProcessor(json: ObjectMapper, syncLastTxnTime: Long => Unit) extends SubmissionPublisher[Value] with Flow.Processor[util.List[ByteBuffer], Value] {
  private val log = LoggerFactory.getLogger(getClass)
  private var subscription: Flow.Subscription = _
  private var subscriber: Flow.Subscriber[_ >: Value] = _

  // We do not request data from the publisher until we have one subscriber
  override def subscribe(subscriber: Flow.Subscriber[_ >: Value]): Unit = {
    if (this.subscriber == null) {
      this.subscriber = subscriber
      super.subscribe(subscriber)
      requestOne()
    } else
      throw new IllegalStateException("BodyValueFlowProcessor can have only one subscriber")
  }

  override def onSubscribe(subscription: Flow.Subscription): Unit =
    this.subscription = subscription

  override def onNext(item: util.List[ByteBuffer]): Unit = {
    import java.nio.charset.StandardCharsets
    val text = item.iterator().asScala.map(StandardCharsets.UTF_8.decode(_).toString).mkString("")
    Try {
      val eventJsonNode = json.readTree(text)
      val value = json.treeToValue[Value](eventJsonNode, classOf[Value])

      // syncLastTxnTime if possible
      value("txn").to[Long].toOpt.foreach(syncLastTxnTime)

      // handle error in stream
      isUnrecoverableError(value, eventJsonNode) match {
        case None => submit(value)
        case Some(unrecoverableError) =>
          subscriber.onError(unrecoverableError) // notify subscriber stream
          subscription.cancel() // cancel subscription on the request body
      }
    }.recover {
      case e: Throwable =>
        log.error(s"could not handle event $text", e)
        subscriber.onError(e) // notify subscriber stream
        subscription.cancel() // cancel subscription on the request body
    }

    requestOne()
  }

  private def isUnrecoverableError(event: Value, jsonNode: JsonNode): Option[Throwable] = {
    (event("type"), Option(jsonNode.get("event"))) match {
      case (VSuccess(StringV("error"), _), Some(error)) =>
        val queryError = json.treeToValue(error, classOf[QueryError])
        val ex = new StreamingException(queryError)
        Some(ex)
      case (VSuccess(StringV("error"), _), None) =>
        Some(new UnknownException(s"unknown error received for event $event", new IllegalArgumentException()))
      case _ => None
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

