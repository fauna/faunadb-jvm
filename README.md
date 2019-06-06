# FaunaDB JVM Drivers

[![Build Status](https://img.shields.io/travis/fauna/faunadb-jvm/master.svg?maxAge=21600)](https://travis-ci.org/fauna/faunadb-jvm)
[![Coverage Status](https://img.shields.io/codecov/c/github/fauna/faunadb-jvm/master.svg?maxAge=21600)](https://codecov.io/gh/fauna/faunadb-jvm/branch/master)
[![Maven Central](https://img.shields.io/maven-central/v/com.faunadb/faunadb-common.svg?maxAge=21600)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.faunadb%22)
[![License](https://img.shields.io/badge/license-MPL_2.0-blue.svg?maxAge=2592000)](https://raw.githubusercontent.com/fauna/faunadb-jvm/master/LICENSE)

This repository contains the FaunaDB drivers for the JVM languages. Currently, Java, Android and Scala clients are implemented.

### Features

* All drivers fully support the current version of the [FaunaDB API](https://faunadb.com/documentation).
* Java and Scala clients share the same underlying library [faunadb-common](./faunadb-common).
* Java and Android clients share the same dsl library [faunadb-java-dsl](./faunadb-java-dsl).
* Supports [Dropwizard Metrics](https://dropwizard.github.io/metrics/3.1.0/) hooks for stats reporting (except Android).
* Support Android 4.1 (API level 16)

## Documentation

Javadocs and Scaladocs are hosted on GitHub:

* [faunadb-java](http://fauna.github.io/faunadb-jvm/1.2.7/faunadb-java/api/)
* [faunadb-java-dsl](http://fauna.github.io/faunadb-jvm/1.2.7/faunadb-java-dsl/api/)
* [faunadb-android](http://fauna.github.io/faunadb-jvm/1.2.7/faunadb-android/api/)
* [faunadb-scala](http://fauna.github.io/faunadb-jvm/1.2.7/faunadb-scala/api/)

## Dependencies

### Shared

* [Jackson](https://github.com/FasterXML/jackson) for JSON parsing.
* [Async HTTP client](https://github.com/AsyncHttpClient/async-http-client) and [Netty](http://netty.io/) for the HTTP transport.
* [Joda Time](http://www.joda.org/joda-time/) for date and time manipulation.

### Android

* [OkHttp client](http://square.github.io/okhttp/) for the HTTP transport.
* [Google Guava](https://github.com/google/guava), for collections and ListenableFutures.

### Java

* Java 7
* [Google Guava](https://github.com/google/guava), for collections and ListenableFutures.

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
    <version>1.2.7</version>
    <scope>compile</scope>
  </dependency>
  ...
</dependencies>
```

##### faunadb-android/pom.xml:

```xml
  <dependencies>
  ...
  <dependency>
    <groupId>com.faunadb</groupId>
    <artifactId>faunadb-android</artifactId>
    <version>1.2.7</version>
    <scope>compile</scope>
  </dependency>
  ...
</dependencies>
```

##### Basic Usage

```java
import com.faunadb.client.*;
import com.faunadb.client.types.*;
import com.faunadb.client.types.Value.*;
import com.google.common.collect.*;

import static com.faunadb.client.query.Language.*;

public class Main {
  public static void main(String[] args) throws Exception {
    FaunaClient client = FaunaClient.builder()
      .withSecret("your-secret-here")
      .build();

    ImmutableList<RefV> indexes = client.query(Paginate(Ref("indexes"))).get()
      .at("data").collect(Field.as(Codec.REF));

    System.out.println(indexes);
  }
}

```

### Scala

#### Installation

##### faunadb-scala/sbt

```scala
libraryDependencies += ("com.faunadb" %% "faunadb-scala" % "1.2.7")
```

##### Basic Usage

```scala
import faunadb.FaunaClient
import faunadb.query._
import faunadb.values._
import scala.concurrent._
import scala.concurrent.duration._

object Main extends App {
  import ExecutionContext.Implicits._

  val client = FaunaClient(secret = "your-secret-here")

  val indexes = client
    .query(Paginate(Ref("indexes")))
    .map(value => value("data").to[Seq[RefV]].get)

  println(
    Await.result(indexes, Duration.Inf)
  )
}
```

## Building

The faunadb-jvm project is built using sbt:

* **sbt**: [Scala Simple Build Tool](http://www.scala-sbt.org/)

To build and run tests against cloud, set the env variable
`FAUNA_ROOT_KEY` to your admin key secret and run `sbt test` from the
project directory.

To run tests against an enterprise cluster or developer instance, you
will also need to set `FAUNA_SCHEME` (http or https), `FAUNA_DOMAIN`
and `FAUNA_PORT`.

### License

All projects in this repository are licensed under the [Mozilla Public License](./LICENSE)
