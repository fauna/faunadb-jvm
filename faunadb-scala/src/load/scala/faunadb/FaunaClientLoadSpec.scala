package faunadb

import java.util
import java.util.concurrent.Flow

import faunadb.query._
import faunadb.values._
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.wordspec.FixtureAsyncWordSpec
import org.scalatest.matchers.should.Matchers

import scala.collection.JavaConverters.asScalaIteratorConverter
import scala.concurrent.{Future, Promise}

class FaunaClientLoadSpec extends FixtureAsyncWordSpec with Matchers with ScalaFutures with IntegrationPatience with FaunaClientFixture {

  "When submitting query whose response is about 10 MB of size, it" should {
    "receive the response with no errors" in { client =>
      // Fixtures
      val collectionName = RandomGenerator.aRandomString

      // Note: below values result in a series of
      // documents that add up 10 MB in size as a whole
      val documentsNumber = 10
      val documentFieldsNumber = 1000
      val documentValuesSize = 1000

      val largeDocument: Expr = {
        val fields: Seq[(String, Expr)] =
          (1 to documentFieldsNumber).map { _ =>
            val name = RandomGenerator.aRandomString
            val value = Expr.encode(RandomGenerator.aRandomString(documentValuesSize))
            (name -> value)
          }

        Obj("data" -> Obj(fields: _*))
      }

      def setup(): Future[(Value, Value)] = {
        def createCollection(): Future[Value] = client.query(CreateCollection(Obj("name" -> collectionName)))

        def createDocuments(): Future[Seq[Value]] = {
          def createDocument(): Future[Value] = client.query(Do(Create(Collection(collectionName), largeDocument), Null))

          Future.sequence(
            (1 to documentsNumber).map { _ =>
              createDocument()
            }
          )
        }

        val result: Future[(Value, Value)] =
          for {
            createCollectionResult <- createCollection()
            createDocumentsResult <- createDocuments()
          } yield (createCollectionResult, createDocumentsResult)

        result
      }

      def run(): Future[Value] = {
        // Get all documents
        client.query {
          Map(
            Paginate(Documents(Collection(collectionName)), size = documentsNumber),
            Lambda(ref => Get(ref))
          )
        }
      }

      // Run
      val result: Future[Value] =
        for {
          _ <- setup()
          result <- run()
        } yield result

      // Verify
      result.map { _ =>
        // TODO: once DRV-250 is done, verify the
        // X-Query-Bytes-Out HTTP Header is greater than 10 MB
        succeed // If result is returned with no errors, succeed
      }
    }
  }

  "When streaming" should {
    // test ignored for now as the client is shared between all tests creating interference
    "buffers events if the producer is faster than the consumer" ignore { client =>
      val subscriberDone = Promise[List[Value]]
      val bufferSize = 256

      // create collection
      val collectionName = RandomGenerator.aRandomString
      val setup = for {
        _ <- client.query(CreateCollection(Obj("name" -> collectionName)))
        createdDoc <- client.query(Create(Collection(collectionName), Obj("credentials" -> Obj("password" -> "abcdefg"))))
        docRef = createdDoc("ref")
        publisherValue <- client.stream(docRef)
      } yield (docRef, publisherValue)

      setup.flatMap { case (docRef, publisherValue) =>
        val valueSubscriber = new Flow.Subscriber[Value] {
          var subscription: Flow.Subscription = _
          val captured = new util.ArrayList[Value]

          override def onSubscribe(s: Flow.Subscription): Unit = {
            subscription = s
            subscription.request(1)
          }

          override def onNext(v: Value): Unit = {
            if (captured.isEmpty) {
              // update doc `bufferSize` times on `start` event
              (1 to bufferSize).iterator
                .map(i => s"testValue$i")
                .foreach { uv =>
                  // blocking call to update document sequentially
                  client.query(Update(docRef, Obj("data" -> Obj("testField" -> uv)))).futureValue
                }
              captured.add(v)
              subscription.request(1) // ask for more elements
            } else {
              captured.add(v) // capture element
              if (captured.size > bufferSize) {
                subscription.cancel()
                subscriberDone.success(captured.iterator().asScala.toList)
              } else {
                subscription.request(1) // ask for more elements
              }
            }
          }

          override def onError(t: Throwable): Unit = subscriberDone.failure(t)

          override def onComplete(): Unit = subscriberDone.failure(new IllegalStateException("not expecting the stream to complete"))
        }

        // subscribe to publisher
        publisherValue.subscribe(valueSubscriber)

        // blocking
        subscriberDone.future.map(_.size should be(bufferSize + 1))
      }
    }
  }

  "When streaming concurrently" should {
    // limit to be raised soon
    "handles at most 100 concurrent streams on the same document" in { client =>
      val concurrentStreamCount = 100
      // create collection
      val collectionName = RandomGenerator.aRandomString
      val setup = for {
        _ <- client.query(CreateCollection(Obj("name" -> collectionName)))
        createdDoc <- client.query(Create(Collection(collectionName), Obj("credentials" -> Obj("password" -> "abcdefg"))))
        docRef = createdDoc("ref")
        // create a first publisher to setup the connection that will be reused by all the other publishers
        firstPublisher <- client.stream(docRef)
        publisherValues <- Future.traverse(List.fill(concurrentStreamCount - 1)(docRef))(ref => client.stream(ref))
        events = (firstPublisher :: publisherValues).map(testSubscriber(4, _))
        // push 3 updates
        _ <- client.query(Update(docRef, Obj("data" -> Obj("testField" -> "testValue1"))))
        _ <- client.query(Update(docRef, Obj("data" -> Obj("testField" -> "testValue2"))))
        _ <- client.query(Update(docRef, Obj("data" -> Obj("testField" -> "testValue3"))))
        subscriberResults <- Future.sequence(events)
      } yield subscriberResults

      setup.map { subscriberResults =>
        subscriberResults.size shouldBe concurrentStreamCount
        subscriberResults.map {
          case startEvent :: t1 :: t2 :: t3 :: Nil =>
            startEvent("type").get shouldBe StringV("start")
            startEvent("event").toOpt.isDefined shouldBe true

            t1("type").get shouldBe StringV("version")
            t1("event", "document", "data").get shouldBe ObjectV("testField" -> StringV("testValue1"))

            t2("type").get shouldBe StringV("version")
            t2("event", "document", "data").get shouldBe ObjectV("testField" -> StringV("testValue2"))

            t3("type").get shouldBe StringV("version")
            t3("event", "document", "data").get shouldBe ObjectV("testField" -> StringV("testValue3"))
          case _ =>
            fail("expected 4 events")
        }
        succeed
      }
    }
  }

  def testSubscriber(messageCount: Int, publisher: Flow.Publisher[Value]): Future[List[Value]] = {
    val capturedEventsP = Promise[List[Value]]

    val valueSubscriber = new Flow.Subscriber[Value] {
      var subscription: Flow.Subscription = _
      val captured = new util.ArrayList[Value]

      override def onSubscribe(s: Flow.Subscription): Unit = {
        subscription = s
        subscription.request(1)
      }
      override def onNext(v: Value): Unit = {
        captured.add(v)
        if (captured.size() == messageCount) {
          capturedEventsP.success(captured.iterator().asScala.toList)
          subscription.cancel()
        } else {
          subscription.request(1)
        }
      }
      override def onError(t: Throwable): Unit = capturedEventsP.failure(t)
      override def onComplete(): Unit = capturedEventsP.failure(new IllegalStateException("not expecting the stream to complete"))
    }
    // subscribe to publisher
    publisher.subscribe(valueSubscriber)
    capturedEventsP.future
  }

}


