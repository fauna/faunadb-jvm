package faunadb

import org.scalatest.{BeforeAndAfterAll, FutureOutcome, SuiteMixin, FixtureAsyncTestSuite}
import scala.concurrent.Future

trait FaunaClientFixture extends SuiteMixin with BeforeAndAfterAll { self: FixtureAsyncTestSuite =>
  private var _config: collection.Map[String, String] = _
  private var _rootClient: FaunaClient = _
  protected def rootClient = _rootClient

  override protected def beforeAll(): Unit = {
    val config = {
      val rootKey = Option(System.getenv("FAUNA_ROOT_KEY")) getOrElse {
        throw new RuntimeException("FAUNA_ROOT_KEY must defined to run tests")
      }
      val domain = Option(System.getenv("FAUNA_DOMAIN")) getOrElse { "db.fauna.com" }
      val scheme = Option(System.getenv("FAUNA_SCHEME")) getOrElse { "https" }
      val port = Option(System.getenv("FAUNA_PORT")) getOrElse { "443" }

      collection.Map("root_token" -> rootKey, "root_url" -> s"$scheme://$domain:$port")
    }

    _config = config
    _rootClient = FaunaClient(endpoint = config("root_url"), secret = config("root_token"))
    super.beforeAll() // To be stackable, must call super.beforeAll
  }

  override type FixtureParam = FaunaClient

  override def withFixture(test: OneArgAsyncTest): FutureOutcome = {
    import faunadb.query._
    import faunadb.values._

    val databaseName = {
      def removeInvalidChars(value: String): String = {
        val validChars = """[;@+$\-_.!~%\w]+"""
        value.filter(_.toString.matches(validChars))
      }

      val randomSuffix = RandomGenerator.aRandomString
      val databaseName = s"$suiteName.${test.name}-$randomSuffix"
      val databaseNameWithValidChars = removeInvalidChars(databaseName)
      databaseNameWithValidChars
    }

    def createDatabase(): Future[Value] = {
      rootClient.query(CreateDatabase(Obj("name" -> databaseName)))
    }

    def createSecret(): Future[String] = {
      rootClient
        .query(CreateKey(Obj("database" -> Database(databaseName), "role" -> "server")))
        .map(_(Field("secret").to[String]).get)
    }

    def createFaunaClient(secret: String): Future[FaunaClient] = Future.successful {
      // each test gets a new client to avoid interference such as triggering max_concurrent_streams error
      val newClient = FaunaClient(endpoint = _config("root_url"), secret = _config("root_token"))
      newClient.sessionClient(secret)
    }

    def deleteDatabase(): Future[Value] = rootClient.query(Delete(Database(databaseName)))

    val result =
      new FutureOutcome(for {
        _ <- createDatabase()
        secret <- createSecret()
        client <- createFaunaClient(secret)
        result <- withFixture(test.toNoArgAsyncTest(client)).toFuture
      } yield result)

    complete {
      result
    }.lastly {
      deleteDatabase()
    }
  }
}