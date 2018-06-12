
## Java Documentation

[The complete Java example is here](SpellExample.java)

#### How to parse query results


Every fauna query returns a `ListenableFuture<Value>`.  Value is the json representation of the query response.  It can be pretty printed by adding a jackson dependency:

```xml
  <dependencies>
  ...
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-core</artifactId>
        <version>2.9.5</version>
    </dependency>
  ...
</dependencies>
```

And then adding the following utility method:

```java
    private static ObjectMapper mapper = getMapper();

    private static ObjectMapper getMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper;
    }

    private static String toPrettyJson(Value value) throws JsonProcessingException {
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
    }
```

Now the return value can be pretty printed by calling:

```java
    Value results = adminClient.query( ....  )
    System.out.println("Query results:\n " + toPrettyJson(results));
```

#### How to create an admin connection to Fauna.

An admin connection should only be used to create top level databases.  After the database is created, a separate client connection should be created.

If you are using the FaunaDB-Cloud version:
 - remove the 'withEndpoint' line below
 - substitute "secret" for your authentication key's secret

```java
    FaunaClient adminClient = FaunaClient.builder()
        .withEndpoint("http://127.0.0.1:8443")
        .withSecret("secret")
        .build();

```

#### How to conditionally create a database

```java
    String DB_NAME = "demo";

    Value dbResults = adminClient.query(
        Do(
            If(
                Exists(Database(DB_NAME)),
                Delete(Database(DB_NAME)),
                Value(true)
            ),
            CreateDatabase(Obj("name", Value(DB_NAME)))
        )
    ).get();
    System.out.println("Successfully created database: " + DB_NAME + ":\n " + toPrettyJson(dbResults));
```

#### How to create a client connection to the database

After the database is created, a new key specific to that database can be used to create a client connection to that database.

```java
    Value keyResults = adminClient.query(
        CreateKey(Obj("database", Database(Value(DB_NAME)), "role", Value("server")))
    ).get();

    String key = keyResults.at("secret").to(String.class).get();
    FaunaClient client = adminClient.newSessionClient(key);
    System.out.println("Connected to Fauna database " + DB_NAME + " with server role\n");
```

#### How to create a class and index

```java
    String SPELLS_CLASS = "spells";
    String INDEX_NAME = "spells_index";

    Value classResults = client.query(
        CreateClass(
            Obj("name", Value(SPELLS_CLASS))
        )
    ).get();
    System.out.println("Create Class for " + DB_NAME + ":\n " + toPrettyJson(classResults) + "\n");

    Value indexResults = client.query(
        CreateIndex(
            Obj("name", Value(INDEX_NAME), "source", Class(Value(SPELLS_CLASS)))
        )
    ).get();
    System.out.println("Create Index for " + DB_NAME + ":\n " + toPrettyJson(indexResults) + "\n");
```

#### How to add entries to a class

```java
    Value addFireResults = client.query(
        Create(
            Class(Value(SPELLS_CLASS)),
            Obj("data",
                Obj("name", Value("Fire Beak"), "element", Value("water"), "cost", Value(15))
            )
        )
    ).get();
    System.out.println("Added spell to class " + SPELLS_CLASS + ":\n " + toPrettyJson(addFireResults) + "\n");

    Value addHippoResults = client.query(
        Create(
            Class(Value(SPELLS_CLASS)),
            Obj("data",
                Obj("name", Value("Hippo's Wallow"), "element", Value("water"), "cost", Value(35))
            )
        )
    ).get();
    System.out.println("Added spell to class " + SPELLS_CLASS + ":\n " + toPrettyJson(addHippoResults) + "\n");

```

#### How to access objects fields and convert to primitive values

Adding data to a class returns a reference to the resource with the reference, a timestamp and the corresponding object in a json structure like:

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
}```

Objects fields are accessed through `at` methods of `Value` class. It's possible to access fields by names if the value represents an object or by index if it represents an array. Also it's possible to convert `Value` class to its primitive correspondent using `to` methods specifying a type.  For example to retrieve the resource reference of the returned Value use the following to get the `ref` field:

```java
    //The results at 'ref' are a resource pointer to the class that was just created.
    Value hippoRef = addHippoResults.at("ref");
    System.out.println("hippoRef = " + hippoRef);
```

#### How to execute a query

The `query` method takes an `Expr` object. `Expr` objects can be composed with others `Expr` to create complex query objects. `com.faunadb.client.query.Language` is a helper class where you can find all available expressions in the library.

```java
    Value getHippoResults = client.query(
        Select(Value("data"),Get(hippoRef))
    ).get();
    System.out.println("Hippo Spells:\n " + toPrettyJson(getHippoResults) + "\n");
```

#### How to retrieve the values from a query result

That query returns the data in the form of a json object.  The data can be extracted from the results by using:

```java
    //convert the hippo results into primitive elements
    String element = getHippoResults.at("element").to(String.class).get();
    System.out.println("spell element = " + element);
```

Later on we will show a better method that uses User Defined types to transform this automatically

#### How to safely work with result objects

This object represents the result of an operation and it might be success or a failure. All conversion operations returns an object like this. This way it's possible to avoid check for nullability everywhere in the code.

```java
    //This would return an empty option if the field is not found or the conversion fails
    Optional<String> optSpellElement = getHippoResults.at("element").to(String.class).getOptional();
    if (optSpellElement.isPresent()) {
        String element2 = optSpellElement.get();
        System.out.println("optional spell element 2 = " + element2);
    }
    else {
        System.out.println("Something went wrong reading the spell");
    }
```

Optionally it's possible transform one `Result<T>` into another `Result<T>` of different type using `map` and `flatMap`.  If the `result` represents an failure all calls to `map` and `flatMap` are ignored and it returns a new failure with the same error message. See `com.faunadb.client.types.Result` for details.


#### How to execute a list query and retrieve a collection of the results

The `query` method takes an `Expr` object. `Expr` objects can be composed with others `Expr` to create complex query objects. `com.faunadb.client.query.Language` is a helper class where you can find all available expressions in the library.

```java
    Value queryIndexResults = client.query(
        SelectAll(Path("data", "id"),
            Paginate(
                Match(Index(Value(INDEX_NAME)))
            ))
    ).get();
```

That query returns a list of resource references to all the spells in the index.  The list of references can be extracted from the results by using:

```java
    Collection<String>  spellsRefIds = queryIndexResults.asCollectionOf(String.class).get();
    System.out.println("spellsRefIds = " + spellsRefIds);
```


### How to work with user defined classes

Instead of manually creating your objects via the DSL (e.g. the Obj()), you can use annotations to automatically encode and decode the class to user-defined types.  These transform the types into the equivalent `Value` types.

For example a Spell class could be used that defines the fields and constructor:

```java
    import com.faunadb.client.types.FaunaConstructor;
    import com.faunadb.client.types.FaunaField;

    public class Spell {

        @FaunaField private String name;
        @FaunaField private String element;
        @FaunaField private int cost;

        @FaunaConstructor
        public Spell(@FaunaField("name") String name, @FaunaField("element") String element, @FaunaField("cost") int cost) {
            this.name = name;
            this.element = element;
            this.cost = cost;
        }

        public String getName() {
            return name;
        }

        public String getElement() {
            return element;
        }

        public int getCost() {
            return cost;
        }

        @Override
        public String toString() {
            return "Spell{" +
                "name='" + name + '\'' +
                ", element='" + element + '\'' +
                ", cost=" + cost +
                '}';
        }
    }
```

There are three attributes that can be used to change the behavior of the `Encoder` and `Decoder`:

- `FaunaField`: Used to override a custom field name and/or provide a default value for that field. If this attribute is not specified, the member name will be used instead. Can be used on fields, properties and constructor arguments.
- `FaunaConstructor`: Used to mark a constructor or a public static method as the method used to instantiate the specified type. This attribute can be used only once per class.
- `FaunaIgnore`: Used to ignore a specific member. Can be used on fields, properties and constructors arguments. If used on a constructor argument, that argument must have a default value.

### Encoding and decoding user defined classes

To persist an instance of `Spell` in FaunaDB:


```java
    Spell newSpell = new Spell("Water Dragon's Claw", "water", 25);
    Value storeSpellResult = client.query(
        Create(
            Class(Value(SPELLS_CLASS)),
            Obj("data", Value(newSpell))
        )
    ).get();
    System.out.println("Stored spell:\n " + toPrettyJson(storeSpellResult));
```

Read the spell we just created and convert from a `Value` type back to the `Spell` type:

```java
    Value dragonRef = storeSpellResult.at("ref");
    Value getDragonResult = client.query(
        Select(Value("data"),
            Get(dragonRef)
        )
    ).get();
    Spell spell = getDragonResult.to(Spell.class).get();
    System.out.println("dragon spell: " + spell);
```

### Encoding and decoding lists of user defined classes

To persist a Java list of `Spell` to FaunaDB encode the list into a `Value`:

```java
    Spell spellOne = new Spell("Chill Touch", "ice", 18);
    Spell spellTwo = new Spell("Dancing Lights", "fire", 45);
    Spell spellThree = new Spell("Fire Bolt", "fire", 32);
    List<Spell> spellList = Arrays.asList(spellOne, spellTwo, spellThree);

    //Lambda Variable for each spell
    String NXT_SPELL = "NXT_SPELL";

    //Encode the list of spells into an expression
    Expr encodedSpellsList = Value(spellList);

    //This query can be approximately read as for each spell in the list of spells evaluate the lambda function.
    //That lambda function creates a temporary variable with each spell in the list and passes it to the create function.
    //The create function then stores that spell in the database
    Value spellsListSave = client.query(
        Foreach(
            encodedSpellsList,
            Lambda(Value(NXT_SPELL),
                Create(
                    Class(Value(SPELLS_CLASS)),
                    Obj("data", Var(NXT_SPELL))
                )
            )
        )
    ).get();

    System.out.println("Created list of spells from java list: \n" + toPrettyJson(spellsListSave));
    Collection<Spell> spellCollection = spellsListSave.asCollectionOf(Spell.class).get();
    System.out.println("save " + spellCollection.size() + " spells:");
    spellCollection.forEach(nextSpell -> System.out.println("   " + nextSpell));
```

Read a list of all `Spells` out of the `Spells` index and decode back to a Java List of `Spells`:

```java
    //Lambda Variable for each spell ref
    String REF_SPELL_ID = "NXT_SPELL";

    //Map is equivalent to a functional map which maps over the set of all values returned by the paginate.
    //Then for each value in the list it runs the lambda function which gets and returns the value.
    //Map returns the data in a structure like this -> {"data": [ {"data": ...}, {"data": ...} ]} so the data field needs to be selected out.
    //SelectAll does this by selecting the nested data with the Path("data", "data")
    Value findAllSpells = client.query(
        SelectAll(Path("data", "data"),
            Map(
                Paginate(
                    Match(Index(Value(INDEX_NAME)))
                ),
                Lambda(Value(REF_SPELL_ID), Get(Var(REF_SPELL_ID))))
        )
    ).get();

    Collection<Spell> allSpellsCollection = findAllSpells.asCollectionOf(Spell.class).get();
    System.out.println("read " + allSpellsCollection.size() + " spells:");
    allSpellsCollection.forEach(nextSpell -> System.out.println("   " + nextSpell));
```
