package faunadb

import faunadb.query._
import faunadb.values._
import org.scalatest.{Matchers, fixture}
import scala.concurrent.Future

class FaunaClientLoadSpec extends fixture.AsyncWordSpec with Matchers with FaunaClientFixture {

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

}


