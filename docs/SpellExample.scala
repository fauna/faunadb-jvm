package com.fauna.learnfauna


import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

/*
 * These are the required imports for Fauna.
 *
 * For these examples we are using the 2.2.0 version of the JVM driver. Also notice that we are doing a global import on
 * the query and values part of the API to make it more obvious we are using Fauna functionality.
 *
 */

import faunadb.FaunaClient
import faunadb.query._
import faunadb.values.{Codec, Value}

object SpellExample  {

  import ExecutionContext.Implicits._

  def main(args: Array[String]): Unit = {

    /*
     * Create an admin connection to FaunaDB.
     *
     * If you are using the FaunaDB-Cloud version:
     *  - remove the 'endpoint' argument below
     *  - substitute "secret" for your authentication key's secret
     */
    val adminClient = FaunaClient("secret", "http://127.0.0.1:8443")

    /*
    * Name of the test database
    */
    val DB_NAME = "demo"

    //query returns a future of a result.  In real production code you should never block on a future and instead
    //handle it async with map, flatMap, etc.
    //Only for demo purposes we are wrapping query with await which blocks for the query to return and gets the result back
    //Never do this in production code.
    val dbResults = await(adminClient.query(
      Do(
        If(
          Exists(Database(DB_NAME)),
          Delete(Database(DB_NAME)),
          true),
        CreateDatabase(Obj("name" -> DB_NAME)
        )
      )
    ))
    println(s"Successfully created database ${dbResults("name").to[String].get} :\n $dbResults \n")

    /*
    *  Create a client connection to the demo database
    */
    val keyResults = await(adminClient.query(
      CreateKey(Obj("database" -> Database(DB_NAME), "role" -> "server"))
    ))
    val key: String = keyResults("secret").to[String].get
    val client = adminClient.sessionClient(key)

    println(s"Connected to Fauna database $DB_NAME with server role\n")
    runSpellExamples(DB_NAME, client)

    /*
     * Just to keep things neat and tidy, delete the database
     */
    deleteDB(adminClient, DB_NAME)
    println("Disconnected from FaunaDB as Admin!")
  }

  private def runSpellExamples(DB_NAME: String, client: FaunaClient): Unit = {

    /*
    * Create the spell collection and index
    */
    val SPELLS_COLLECTION = "spells"
    val INDEX_NAME = "spells_index"

    val collectionResults: Value = await(client.query(CreateCollection(Obj("name" -> SPELLS_COLLECTION))))
    println(s"Create Collection for $DB_NAME:\n $collectionResults\n")

    val indexResults: Value = await(client.query(
      CreateIndex(
        Obj("name" -> INDEX_NAME,
          "source" -> Collection(SPELLS_COLLECTION)
        )
      )
    ))
    println(s"Create Index for $DB_NAME:\n $indexResults\n")

    /*
    * Add some entries to the spells collection
    */
    val addFireResults = await(client.query(
      Create(Collection(Value(SPELLS_COLLECTION)),
        Obj("data" ->
          Obj("name" -> "Fire Beak", "element" -> "water", "cost" -> 15)))
    ))

    println(s"Added spell to collection $SPELLS_COLLECTION: \n $addFireResults \n")

    val addDragonResults = await(client.query(
      Create(Collection(Value(SPELLS_COLLECTION)),
        Obj("data" ->
          Obj("name" -> "Water Dragon's Claw", "element" -> "water")
        )
      )
    ))
    println(s"Added spell to collection $SPELLS_COLLECTION \n $addDragonResults \n")

    val addHippoResults = await(client.query(
      Create(Collection(Value(SPELLS_COLLECTION)),
        Obj("data" ->
          Obj("name" -> "Hippo's Wallow", "element" -> "water", "cost" -> 35)))
    ))
    println(s"Added spell to collection $SPELLS_COLLECTION:\n $addHippoResults \n")

    //The results at 'ref' are a pointer to the document of the collection that was just created.
    val hippoRef = addHippoResults("ref")
    println(s"hippoRef = $hippoRef \n")

    /*
    * Read the hippo back that we just created
    */
    val getHippoResults: Value = await(client.query(
      Select(Value("data"), Get(hippoRef))
    ))
    println(s"Hippo Spell:\n $getHippoResults \n")

    //convert the hippo results into primitive elements
    val name: String = getHippoResults("name").to[String].get
    val cost: Int = getHippoResults("cost").to[Int].get
    val element: String = getHippoResults("element").to[String].get
    println(s"Spell Details: Name=$name, Cost=$cost, Element=$element")

    //This would return an empty option if the field is not found or if the conversion fails
    val optSpellElement: Option[String] = getHippoResults("element").to[String].toOpt
    optSpellElement match {
      case Some(element2) => println(s"optional spell element $element2")
      case None => println("Something went wrong reading the spell")
    }

    /*
     * Query for all the spells in the index
     */
    val queryIndexResults: Value = await(client.query(
      SelectAll(Path("data", "id"),
        Paginate(
          Match(Index(Value(INDEX_NAME)))
        )
      )
    ))

    val spellsRefIds: Seq[String] = queryIndexResults.to[Seq[String]].getOrElse(Seq.empty)
    println(s"spellsRefIds = $spellsRefIds \n")

    /*
    * Store a Spell case class
    */
    val newSpell = Spell("Water Dragon's Claw", "water", Option(25))
    val storeSpellResult = await(client.query(
      Create(
        Collection(SPELLS_COLLECTION),
        Obj("data" -> newSpell))
    ))
    println(s"Stored spell:\n $storeSpellResult \n")

    /*
     * Read the spell we just created
     */
    val dragonRef = storeSpellResult("ref")
    val getDragonResult = await(client.query(
      Select(
        Value("data"),
        Get(dragonRef)
      )
    ))

    val spell = getDragonResult.to[Spell].get
    println(s"dragon spell: $spell \n")

    /*
    * Example of using proper Future mapping to handle results async.  Could be map, flatMap or a for expression
    */
    for {
      dragonSpellVal <- client.query(Select(Value("data"), Get(dragonRef)))
      dragonSpell = dragonSpellVal.to[Spell].get
      hippoSpellVal <- client.query(Select(Value("data"), Get(hippoRef)))
      hippoSpell = hippoSpellVal.to[Spell].get
    }
    yield {
      //process all spells retrieved
      println(s"Retrieved spells of $dragonSpell and $hippoSpell")
    }

    /*
    * Store a list of Spells
    */
    val spellOne = Spell("Chill Touch", "ice", Option(18))
    val spellTwo = Spell("Dancing Lights", "fire", Option(45))
    val spellThree = Spell("Fire Bolt", "fire", Option(32))
    val spellList = Seq(spellOne, spellTwo, spellThree)

    //This query can be approximately read as for each spell in the list of spells evaluate the lambda function.
    //That lambda function creates a temporary variable with each spell in the list and passes it to the create function.
    //The create function then stores that spell in the database
    val spellsListSave = await(client.query(
      Foreach(spellList,
        Lambda { nextSpell =>
          Create(
            Collection(Value(SPELLS_COLLECTION)),
            Obj("data" -> nextSpell))
        })
    ))

    println(s"Created list of spells from java list: \n $spellsListSave \n")
    val spellCollection = spellsListSave.to[Seq[Spell]].get
    println(s"saved ${spellCollection.size} spells:")
    spellCollection.foreach((nextSpell: Spell) => println(s"   $nextSpell"))

    println("\n")

    /*
     * Read all Spells for the Spells Index
     */

    val findAllSpells = await(client.query(
      SelectAll("data" / "data",
        Map(
          Paginate(Match(Index(Value(INDEX_NAME)))),
          Lambda { x => Get(x) }
        )
      )
    ))

    println(s"findAllSpells = $findAllSpells\n")

    val allSpellsCollection = findAllSpells.to[Seq[Spell]].get
    println(s"read ${allSpellsCollection.size} spells:")
    allSpellsCollection.foreach((nextSpell: Spell) => println(s"   $nextSpell"))

    println("\n")
  }

  case class Spell(name: String, element: String, cost: Option[Int])

  object Spell {
    implicit val spellCodec: Codec[Spell] = Codec.caseClass[Spell]
  }

  def deleteDB(adminClient: FaunaClient, dbName: String): Unit = {
    /*
    * Delete the Database created
    */
    val result = await(
      adminClient.query(
        If(
          Exists(Database(dbName)),
          Delete(Database(dbName)),
          Value(true))
      ))
    println(s"Deleted database: $dbName:\n $result\n")
  }

  def await[T](f: Future[T]): T = Await.result(f, 5.second)
}

