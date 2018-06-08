
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
    Value results = adminClient.query( ....
    System.out.println("Query results:\n " + toPrettyJson(results));
```

#### How to create an admin connection to Fauna.

An admin connection should only be used to create top level databases.  After the database is created, a separate client connection should be created.

If you are using the FaunaDB-Cloud version:
 - remove the 'withEndpoint line below
 - substitute your secret for "secret" below

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
        Arr(
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
    //tidy things up by closing the admin connection
    adminClient.close();
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
            Obj("name", Value(INDEX_NAME), "source", Class(Value(SPELLS_CLASS))
            )
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

Objects fields are accessed through `at` methods of `Value` class. It's possible to access fields by names if the value represents an object or by index if it represents an array. Also it's possible to convert `Value` class to its primitive correspondent using `to` methods specifying a type.  For example to retrieve the resource reference of the object use the following to get the `ref` field and convert it to a RefV:

```java
    Value.RefV hippoRef = addHippoResults.at("ref").to(Value.RefV.class).get();
    System.out.println("hippoRef = " + hippoRef);
```

#### How to safely work with result objects

This object represents the result of an operation and it might be success or a failure. All conversion operations returns an object like this. This way it's possible to avoid check for nullability everywhere in the code.

```java
    Optional<Value.RefV> optHippoRef = addHippoResults.at("ref").to(Value.RefV.class).getOptional();
    if (optHippoRef.isPresent()) {
        System.out.println("hippoRef id = " + optHippoRef.get().getId());
        processHippos(optHippoRef.get());
    }
    else {
        System.out.println("Something went wrong creating the hippo");
    }
```

Optionally it's possible transform one `Result<T>` into another `Result<T>` of different type using `map` and `flatMap`:

If the `result` represents an failure all calls to `map` and `flatMap` are ignored and it returns a new failure with the same error message. See `com.faunadb.client.types.Result` for details.

```java
    String refID = addHippoResults.at("ref").to(Value.RefV.class).map(ref1 -> ref1.getId()).get();
    System.out.println("refID = " + refID);

```

#### How to execute a query

The `query` method takes an `Expr` object. `Expr` objects can be composed with others `Expr` to create complex query objects. `com.faunadb.client.query.Language` is a helper class where you can find all available expressions in the library.

```java
    Value getHippoResults = client.query(
        Get(Ref(
            Class(SPELLS_CLASS),
            Value(hippoRef.getId())
            )
        )
    ).get();
    System.out.println("Hippo Spells:\n " + toPrettyJson(getHippoResults) + "\n");
```

#### How to retrieve the values from a query result

That query returns the data in the form of a json object.  The data can be extracted from the results by using:

```java
    Value data = getHippoResults.get(Field.at("data"));
    String element = data.get(Field.at("element")).to(String.class).get();
    System.out.println("spell element = " + element);

    Value.ObjectV objectV = getHippoResults.at("data").to(Value.ObjectV.class).get();
    System.out.println("objectV = " + objectV);
```

Later on we will show a better method that uses User Defined types to transform this automatically


#### How to execute a list query and retrieve a collection of the results

The `query` method takes an `Expr` object. `Expr` objects can be composed with others `Expr` to create complex query objects. `com.faunadb.client.query.Language` is a helper class where you can find all available expressions in the library.

```java
    Value queryIndexResults = client.query(
        Select(Value("data"),
            Paginate(
            Match(Index(Value(INDEX_NAME)))
        ))
    ).get();
    System.out.println("All spells:\n " + toPrettyJson(queryIndexResults) + "\n");
```

That query returns a list of resource references to all the spells in the index.  The list of references can be extracted from the results by using:

```java
    Collection<Value.RefV> refVCollection = queryIndexResults.asCollectionOf(Value.RefV.class).get();
    List<String> listOfIds = refVCollection.stream().map(refV -> refV.getId()).collect(Collectors.toList());
    System.out.println("Spell ref ids = " + listOfIds);
```


### How to work with user defined classes

Instead of manually creating your objects via the DSL (e.g. the ObjectV), you can use annotations to automatically encode and decode the class to user-defined types.  These transform the types into the equivalent `Value` types.

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

Then a spell can be saved using:

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

Read the spell we just created:

```java
    Value.RefV dragonRef = storeSpellResult.at("ref").to(Value.RefV.class).get();
    Value getDragonResult = client.query(
        Select(Value("data"),
            Get(Ref(
                Class(SPELLS_CLASS),
                Value(dragonRef.getId())
                )
            )
        )
    ).get();
    Spell spell = getDragonResult.to(Spell.class).get();
    System.out.println("dragon spell: " + spell);
```