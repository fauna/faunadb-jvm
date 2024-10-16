# Java and Scala drivers for Fauna v4 (deprecated)

[![Maven Central](https://img.shields.io/maven-central/v/com.faunadb/faunadb-common.svg?maxAge=21600)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.faunadb%22)
[![License](https://img.shields.io/badge/license-MPL_2.0-blue.svg?maxAge=2592000)](https://raw.githubusercontent.com/fauna/faunadb-jvm/main/LICENSE)

> [!WARNING]
>  Fauna is decommissioning FQL v4 on June 30, 2025.
>
> This driver is not compatible with FQL v10, the latest version. Fauna accounts
> created after August 21, 2024 must use FQL v10. Ensure you migrate existing
> projects to the official v10 driver by the v4 EOL date:
> https://github.com/fauna/fauna-jvm.
>
> For more information, see the [v4 end of life (EOL)
> announcement](https://docs.fauna.com/fauna/v4/#fql-v4-end-of-life) and
> [related FAQ](https://docs.fauna.com/fauna/v4/migration/faq).

This repository contains the official Java and Scala drivers for [Fauna
v4](https://docs.fauna.com/fauna/v4/).

See the [Fauna v4 documentation](https://docs.fauna.com/fauna/v4) and
[tutorials](https://docs.fauna.com/fauna/v4/learn/tutorials/fql/crud?lang=javascript)
for guides and a complete database [API
reference](https://docs.fauna.com/fauna/v4/api/fql/).

### Features

* All drivers fully support the current version of the [FaunaDB API](https://docs.fauna.com/fauna/current/reference/queryapi/).
* Java and Scala clients share the same underlying library [faunadb-common](./faunadb-common).
* Supports [Dropwizard Metrics](https://dropwizard.github.io/metrics/3.1.0/) hooks for stats reporting.

## Documentation

Javadocs and Scaladocs are hosted on GitHub:

* [faunadb-java](http://fauna.github.io/faunadb-jvm/4.5.0/faunadb-java/api/)
* [faunadb-scala](http://fauna.github.io/faunadb-jvm/4.5.0/faunadb-scala/api/)

Details Documentation for each language:

* [Java](docs/java.md)
* [Scala](docs/scala.md)

## Dependencies

### Shared

* [Jackson](https://github.com/FasterXML/jackson) for JSON parsing.

### Java

* Java 11

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
    <version>4.5.0</version>
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
    }
}
```

##### Per query metrics
There are several metrics that returned per each request in response headers. Read this [doc](https://docs.fauna.com/fauna/current/concepts/billing.html#perquery) to have more info.
You can access these metrics by:
```java
import com.faunadb.client.FaunaClient;

import static com.faunadb.client.query.Language.*;

public class Main {
    public static void main(String[] args) throws Exception {

        //Create an admin connection to FaunaDB.
        FaunaClient adminClient =
            FaunaClient.builder()
                .withSecret("put-your-key-secret-here")
                .build();

        MetricsResponse metricsResponse = adminClient.queryWithMetrics(
            Paginate(Match(Index("spells_by_element"), Value("fire"))),
            Optional.empty()
    	).get();

        // the result of the query, as if you invoke 'adminClient.query' function instead
        Value value = metricsResponse.getValue();

        // gets the value of 'x-byte-read-ops' metric
        Optional<String> byteReadOps = metricsResponse.getMetric(MetricsResponse.Metrics.BYTE_READ_OPS);
        // gets the value of 'x-byte-write-ops' metric
        Optional<String> byteWriteOps = metricsResponse.getMetric(MetricsResponse.Metrics.BYTE_WRITE_OPS);

        // you can get other metrics in the same way,
        // all of them are exposed via MetricsResponse.Metrics enum
    }
}
```

##### Custom headers
There is an optional possibility to send custom headers for each http request. They can be defined in the client's constructor:
```java
FaunaClient adminClient =
    FaunaClient.builder()
        .withSecret("put-your-key-secret-here")
        .withCustomHeaders(
            Map.of(
                "custom-header-1", "value-1",
                "custom-header-2", "value-2"
            )
        )
        .build();
```

##### Document Streaming

Fauna supports document streaming, where changes to a streamed document are pushed to all clients subscribing to that document.

The streaming API is built using the [java.util.concurrent.Flow](https://docs.oracle.com/javase/9/docs/api/java/util/concurrent/Flow.html) which enable users to establish flow-controlled components in which `Publishers` produce items consumed by one or more `Subscribers`, each managed by a `Subscription`.

The following example assumes that you have already created a `FaunaClient`.

In the example below, we are capturing the 4 first messages by manually binding a `Subscriber`.

```java
// docRef is a reference to the document for which we want to stream updates.
// You can acquire a document reference with a query like the following, but it
// needs to work with the documents that you have.
// Value docRef = Ref(Collection("scoreboards"), "123")

Flow.Publisher<Value> valuePublisher = adminClient.stream(createdDoc).get();
CompletableFuture<List<Value>> capturedEvents = new CompletableFuture<>();

Flow.Subscriber<Value> valueSubscriber = new Flow.Subscriber<>() {
  Flow.Subscription subscription = null;
  ArrayList<Value> captured = new ArrayList<>();
  @Override
  public void onSubscribe(Flow.Subscription s) {
    subscription = s;
    subscription.request(1);
  }

  @Override
  public void onNext(Value v) {
    captured.add(v);
    if (captured.size() == 4) {
      capturedEvents.complete(captured);
      subscription.cancel();
    } else {
      subscription.request(1);
    }
  }

  @Override
  public void onError(Throwable throwable) {
     capturedEvents.completeExceptionally(throwable);
  }

  @Override
  public void onComplete() {
    capturedEvents.completeExceptionally(new IllegalStateException("not expecting the stream to complete"));
  }
};

// subscribe to publisher
valuePublisher.subscribe(valueSubscriber);

// blocking
List<Value> events = capturedEvents.get();
```

[Detailed Java Documentation can be found here](docs/java.md)

### Scala

#### Installation

##### faunadb-scala/sbt

```scala
libraryDependencies += ("com.faunadb" %% "faunadb-scala" % "4.5.0")
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
}
```

##### Per query metrics
There are several metrics that returned per each request in response headers. Read this [doc](https://docs.fauna.com/fauna/current/concepts/billing.html#perquery) to have more info.
You can access these metrics by:
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

  val metricsResponse = client.queryWithMetrics(
    Paginate(Match(Index("spells_by_element"), Value("fire"))),
    None
  ).futureValue

  // the result of the query, as if you invoke 'client.query' function instead
  val value = metricsResponse.value

  // gets the value of 'x-byte-read-ops' metric
  val byteReadOps = metricsResponse.getMetric(Metrics.ByteReadOps)
  // gets the value of 'x-byte-write-ops' metric
  val byteWriteOps = metricsResponse.getMetric(Metrics.ByteWriteOps)

  // you can get other metrics in the same way,
  // all of them are exposed via Metrics enum}
```

##### Custom headers
There is an optional possibility to send custom headers for each http request. They can be defined in the client's constructor:
```scala
val client = FaunaClient(
  secret = "put-your-secret-here",
  customHeaders = scala.Predef.Map("custom-header-1" -> "value-1", "custom-header-2" -> "value-2")
)
```

##### Document Streaming

Fauna supports document streaming, where changes to a streamed document are pushed to all clients subscribing to that document.

The following sections provide examples for managing streams with Flow or Monix, and
assume that you have already created a `FaunaClient`.

###### Flow subscriber

It is possible to use the [java.util.concurrent.Flow](https://docs.oracle.com/javase/9/docs/api/java/util/concurrent/Flow.html) API directly by binding a `Subscriber` manually.

In the example below, we are capturing the 4 first messages:

```scala
import faunadb._
import faunadb.query._

// docRef is a reference to the document for which we want to stream updates.
// You can acquire a document reference with a query like the following, but it
// needs to work with the documents that you have.
// val docRef = Ref(Collection("scoreboards"), "123")

client.stream(docRef).flatMap { publisher =>
  // Promise to hold the final state
  val capturedEventsP = Promise[List[Value]]

  // Our manual Subscriber
  val valueSubscriber = new Flow.Subscriber[Value] {
    var subscription: Flow.Subscription = null
    val captured = new ConcurrentLinkedQueue[Value]

    override def onSubscribe(s: Flow.Subscription): Unit = {
      subscription = s
      subscription.request(1)
    }

    override def onNext(v: Value): Unit = {
      captured.add(v)
      if (captured.size() == 4) {
        capturedEventsP.success(captured.iterator().asScala.toList)
        subscription.cancel()
      } else {
        subscription.request(1)
      }
    }

    override def onError(t: Throwable): Unit =
      capturedEventsP.failure(t)

    override def onComplete(): Unit =
      capturedEventsP.failure(new IllegalStateException("not expecting the stream to complete"))
  }
  // subscribe to publisher
  publisher.subscribe(valueSubscriber)
  // wait for Future completion
  capturedEventsP.future
}
```

###### Monix

The [reactive-streams](http://www.reactive-streams.org/) standard offers a strong interoperability in the streaming ecosystem.

We can replicate the previous example using the [Monix](https://monix.io/) streaming library.

```scala
import faunadb._
import faunadb.query._
import monix.execution.Scheduler
import monix.reactive.Observable
import org.reactivestreams.{FlowAdapters, Publisher}

// docRef is a reference to the document for which we want to stream updates.
// You can acquire a document reference with a query like the following, but it
// needs to work with the documents that you have.
// val docRef = Ref(Collection("scoreboards"), "123")

client.stream(docRef).flatMap { publisher =>
  val reactiveStreamsPublisher: Publisher[Value] = FlowAdapters.toPublisher(publisherValue)
  Observable.fromReactivePublisher(reactiveStreamsPublisher)
    .take(4) // 4 events
    .toListL
    .runToFuture(Scheduler.Implicits.global)
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
