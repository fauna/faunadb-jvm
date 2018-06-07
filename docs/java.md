
## Java Documentation

#### How to parse query results


Every fauna query returns a `ListenableFuture<Value>`.  Valu is the json representation of the query response.  It can be pretty printed by adding a jackson dependency:

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
     *  - remove the 'withEndpoint line below
     *  - substitute your secret for "secret" below

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




```csharp
Value result = await client.Query(Paginate(Match(Index("spells"))));
```

`Query` methods receives an `Expr` object. `Expr` objects can be composed with others `Expr` to create complex query objects. `FaunaDB.Query.Language` is a helper class where you can find all available expressions in the library.

#### How to access objects fields and convert to primitive values

Objects fields are accessed through `At` methods of `Value` class. It's possible to access fields by names if the value represents an object or by index if it represents an array. Also it's possible to convert `Value` class to its primitive correspondent using `To` methods specifying a type.

```csharp
IResult<Value[]> data = result.At("data").To<Value[]>();
```

#### How work with `IResult<T>` objects

This object represents the result of an operation and it might be success or a failure. All convertion operations returns an object like this. This way it's possible to avoid check for nullability everywhere in the code.

```csharp
data.Match(
    Success: value => ProcessData(value),
    Failure: reason => Console.WriteLine($"Something went wrong: {reason}")
);
```

Optionally it's possible transform one `IResult<T>` into another `IResult<U>` of different type using `Map` and `FlatMap`.

```csharp
IResult<int> result = <<...>>;
IResult<string> result.Map(value => value.toString());
```

If `result` represents an failure all calls to `Map` and `FlatMap` are ignored. See `FaunaDB.Types.Result`.
