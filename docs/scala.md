## Scala Documentation

[The complete Scala example is here](SpellExample.scala)

### Handling Fauna query results

A fauna query returns a future of a result.  In real production code you should never block on a future and instead handle it async with map, flatMap, etc.

Only for demo purposes we are wrapping queries with await which blocks for the query to return and gets the result back.  Never do this in production code.

This small helper method is used to make wrapping every call in await easier:

```scala
    def await[T](f: Future[T]): T = Await.result(f, 5.second)
```

### How to create an admin connection to Fauna.

An admin connection should only be used to create top level databases.  After the database is created, a separate client connection should be created.

If you are using the FaunaDB-Cloud version:
 - remove the 'endpoint' argument below
 - substitute "secret" for your authentication key's secret

```scala
   val adminClient = FaunaClient("secret", "http://127.0.0.1:8443")
```

### How to conditionally create a database

```scala
    val DB_NAME = "demo"

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
```

### How to create a client connection to the database

After the database is created, a new key specific to that database can be used to create a client connection to that database.

```scala
    val keyResults = await(adminClient.query(
      CreateKey(Obj("database" -> Database(DB_NAME), "role" -> "server"))
    ))
    val key: String = keyResults("secret").to[String].get
    val client = adminClient.sessionClient(key)
```

### How to create a collection and index

```scala
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
```

### How to add entries to a collection

```scala
    val addFireResults = await(client.query(
      Create(Collection(Value(SPELLS_COLLECTION)),
        Obj("data" ->
          Obj("name" -> "Fire Beak", "element" -> "water", "cost" -> 15)))
    ))
    println(s"Added spell to collection $SPELLS_COLLECTION: \n $addFireResults \n")

    val addHippoResults = await(client.query(
      Create(Collection(Value(SPELLS_COLLECTION)),
        Obj("data" ->
          Obj("name" -> "Hippo's Wallow", "element" -> "water", "cost" -> 35)))
    ))
    println(s"Added spell to collection $SPELLS_COLLECTION:\n $addHippoResults \n")
```

### How to access objects fields and convert to primitive values

Adding data to a collection returns a reference to the resource with the reference, a timestamp and the corresponding object in a json structure like:

```json
 {
  "object" : {
    "ref" : {
      ....
    },
    "ts" : 1528414251402950,
    "data" : {
      ....
    }
  }
}
```

Objects fields are accessed through the default method of `Value` class. It's possible to access fields by names if the value represents an object or by index if it represents an array. For example to retrieve the resource reference of the returned Value use the following to get the `ref` field:

```scala
    //The results at 'ref' are a pointer to the document of the collection that was just created.
    val hippoRef = addHippoResults("ref")
    println(s"hippoRef = $hippoRef \n")
```

### How to execute a query

The `query` method takes an `Expr` object. `Expr` objects can be composed with others `Expr` to create complex query objects. `faunadb.query` is a helper package where you can find all available expressions in the library.

```scala
    Value readHippoResults = client.query(
        Select(Value("data"),Get(hippoRef))
    ).get();
    System.out.println("Hippo Spells:\n " + readHippoResults + "\n");
```

The `query` method also accepts a `timeout` implicit parameter. The `timeout` value defines the maximum time a `query` will be allowed to run on the server. If the value is exceeded, the query is aborted. If no `timeout` is defined in scope, a default value is assigned on the server side.

```scala
import scala.concurrent.duration._

implicit val timeout = 500 millis

client.query(
  Select(Value("data"), Get(hippoRef))
)
```

### How to retrieve the values from a query result

That query returns the data in the form of a json object. It's possible to convert `Value` class to its primitive correspondent using `to` methods specifying a type.  For example the data can be extracted from the results by using:

```scala
    //convert the hippo results into primitive elements
    val name: String = getHippoResults("name").to[String].get
    val cost: Int = getHippoResults("cost").to[Int].get
    val element: String = getHippoResults("element").to[String].get
    println(s"Spell Details: Name=$name, Cost=$cost, Element=$element")
```

Later on we will show a better method for converting to native types with User Defined types that do this transformation automatically.

### How to safely work with result objects

This object represents the result of an operation and it might be success or a failure. All conversion operations returns an object like this. This way it's possible to avoid check for nullability everywhere in the code.

```scala
    //This would return an empty option if the field is not found or if the conversion fails
    val optSpellElement: Option[String] = getHippoResults("element").to[String].toOpt
    optSpellElement match {
      case Some(element2) => println(s"optional spell element $element2")
      case None => println("Something went wrong reading the spell")
    }
```

Optionally it's possible transform one `Result[T]` into another `Result[T]` of different type using `map` and `flatMap`.  If the `result` represents an failure all calls to `map` and `flatMap` are ignored and it returns a new failure with the same error message. See `faunadb.values.Result` for details.

### How to execute a list query and retrieve a collection of the results

The `query` method takes an `Expr` object. `Expr` objects can be composed with others `Expr` to create complex query objects. `faunadb.query` is a helper package where you can find all available expressions in the library.

```scala
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
```

That query returns a list of resource references to all the spells in the index.  The list of references can be extracted from the results by using:

```scala
    val spellsRefIds: Seq[String] = queryIndexResults.to[Seq[String]].getOrElse(Seq.empty)
    println(s"spellsRefIds = $spellsRefIds \n")
```


### How to work with user defined classes

Instead of manually creating your objects via the DSL (e.g. the Obj()), you can use case classes and codec to automatically encode and decode the class to user-defined types.  These transform the types into the equivalent `Value` types.

For example a Spell case class could be used that defines the fields and constructor:

```scala
 case class Spell(name: String, element: String, cost: Option[Int])

  object Spell {
    implicit val spellCodec: Codec[Spell] = Codec.caseClass[Spell]
  }
```

Option is used to mark a field that is optional and might not have a value


### Encoding and decoding user defined classes

To persist a document of `Spell` in FaunaDB:


```scala
    val newSpell = Spell("Water Dragon's Claw", "water", Option(25))
    val storeSpellResult = await(client.query(
      Create(
        Collection(SPELLS_COLLECTION),
        Obj("data" -> newSpell))
    ))
    println(s"Stored spell:\n $storeSpellResult \n")
```

Read the spell we just created and convert from a `Value` type back to the `Spell` type:

```scala
    val dragonRef = storeSpellResult("ref")
    val getDragonResult = await(client.query(
      Select(
        Value("data"),
        Get(dragonRef)
      )
    ))

    val spell = getDragonResult.to[Spell].get
    println(s"dragon spell: $spell \n")
```

### Example of using a proper Future mapping to handle results async.  Could be map, flatMap or a for expression.

```scala
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
```

### Encoding and decoding lists of user defined collections

To persist a Scala sequence of `Spell` to FaunaDB because a codec is defined for the list it can be directly passed to the query Foreach.  This will convert it into a `Value` type:

```scala
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

```

Read a list of all `Spells` out of the `Spells` index and decode back to a Scala sequence of `Spells`:

```scala
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
```
