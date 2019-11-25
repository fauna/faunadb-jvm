# FaunaDB JVM Drivers

[![Maven Central](https://img.shields.io/maven-central/v/com.faunadb/faunadb-common.svg?maxAge=21600)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.faunadb%22)
[![License](https://img.shields.io/badge/license-MPL_2.0-blue.svg?maxAge=2592000)](https://raw.githubusercontent.com/fauna/faunadb-jvm/master/LICENSE)

This repository contains the FaunaDB drivers for the JVM languages. Currently, Java and Scala clients are implemented.

### Features

* All drivers fully support the current version of the [FaunaDB API](https://docs.fauna.com/fauna/current/reference/queryapi/).
* Java and Scala clients share the same underlying library [faunadb-common](./faunadb-common).
* Supports [Dropwizard Metrics](https://dropwizard.github.io/metrics/3.1.0/) hooks for stats reporting.

## Documentation

Javadocs and Scaladocs are hosted on GitHub:

* [faunadb-java](http://fauna.github.io/faunadb-jvm/2.9.0/faunadb-java/api/)
* [faunadb-scala](http://fauna.github.io/faunadb-jvm/2.9.0/faunadb-scala/api/)

Details Documentation for each language:

* [Java](docs/java.md)
* [Scala](docs/scala.md)

## Dependencies

### Shared

* [Jackson](https://github.com/FasterXML/jackson) for JSON parsing.
* [Async HTTP client](https://github.com/AsyncHttpClient/async-http-client) and [Netty](http://netty.io/) for the HTTP transport.
* [Joda Time](http://www.joda.org/joda-time/) for date and time manipulation.

### Java

* Java 8

### Scala

* Scala 2.11.x
* Scala 2.12.x

## Using the Driver

### Java

#### Installation

Download from the Maven central repository:

##### faunadb-java/pom.xml:

```xml
  <dependencies>
  ...
  <dependency>
    <groupId>com.faunadb</groupId>
    <artifactId>faunadb-java</artifactId>
    <version>2.9.0</version>
    <scope>compile</scope>
  </dependency>
  ...
</dependencies>
```

##### Basic Java Usage

```java
import com.faunadb.client.FaunaClient;

import static com.faunadb.client.query.Language.*;

/**
 * This example connects to FaunaDB Cloud using the secret provided
 * and creates a new database named "my-first-database"
 */
public class Main {
    public static void main(String[] args) throws Exception {

        //Create an admin connection to FaunaDB.
        FaunaClient adminClient =
            FaunaClient.builder()
                .withSecret("put-your-key-secret-here")
                .build();

        adminClient.query(
            CreateDatabase(
                Obj("name", Value("my-first-database"))
            )
        ).get();

        client.close();
    }
}
```

[Detailed Java Documentation can be found here](docs/java.md)

### Scala

#### Installation

##### faunadb-scala/sbt

```scala
libraryDependencies += ("com.faunadb" %% "faunadb-scala" % "2.9.0")
```

##### Basic Usage

```scala
import faunadb._
import faunadb.query._
import scala.concurrent._
import scala.concurrent.duration._

/**
  * This example connects to FaunaDB Cloud using the secret provided
  * and creates a new database named "my-first-database"
  */
object Main extends App {

  import ExecutionContext.Implicits._

  val client = FaunaClient(
    secret = "put-your-secret-here"
  )

  val result = client.query(
    CreateDatabase(
      Obj("name" -> "my-first-database")
    )
  )

  Await.result(result, Duration.Inf)

  client.close()
}
```

## Building

The faunadb-jvm project is built using sbt:

* **sbt**: [Scala Simple Build Tool](http://www.scala-sbt.org/)

To build and run tests against cloud, set the env variable
`FAUNA_ROOT_KEY` to your admin key secret and run `sbt test` from the
project directory.

Alternatively, tests can be run via a Docker container with
`FAUNA_ROOT_KEY="your-cloud-secret" make docker-test` (an alternate
Debian-based JDK image can be provided via `RUNTIME_IMAGE`).

To run tests against an enterprise cluster or developer instance, you
will also need to set `FAUNA_SCHEME` (http or https), `FAUNA_DOMAIN`
and `FAUNA_PORT`.

### License

All projects in this repository are licensed under the [Mozilla Public License](./LICENSE)
