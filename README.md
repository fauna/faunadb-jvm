# FaunaDB JVM Clients

This repository contains the FaunaDB clients for the JVM languages. Currently, Java and Scala clients are implemented.

### Features

* All clients fully support the current version of the [FaunaDB API](https://faunadb.com/documentation).
* All per-language clients share the same underlying transport library [faunadb-httpclient](./faunadb-httpclient).
* Supports [Dropwizard Metrics](https://dropwizard.github.io/metrics/3.1.0/) hooks for stats reporting.

## Maven/Ivy

Coming soon.

## Documentation

Javadocs and Scaladocs are hosted on GitHub:

* [faunadb-java](http://faunadb.github.io/faunadb-jvm/faunadb-java/api/)
* [faunadb-scala](http://faunadb.github.io/faunadb-jvm/faunadb-scala/api/)

## Dependencies

### Shared

* [Jackson](https://github.com/FasterXML/jackson) for JSON parsing.
* [Async HTTP client](https://github.com/AsyncHttpClient/async-http-client) and [Netty](http://netty.io/) for the HTTP transport.

### Java

* Java 7
* [Google Guava](https://github.com/google/guava), for collections and ListenableFutures.

### Scala

* Scala 2.11.x

## Building

### Build Dependencies

* **sbt**: [Scala Simple Build Tool](http://www.scala-sbt.org/)

### Building and Using

1. Clone this repository.
2. Run `sbt package` to build all JAR files needed.
3. Copy the `faunadb-httpclient/target/faunadb-httpclient-0.1-SNAPSHOT.jar` to your project's unmanaged library directory.
4. Copy the client JAR of your choice to your project's unmanaged library directory. `faunadb-java/target/faunadb-java-0.1-SNAPSHOT.jar`
for the Java client, and `faunadb-scala/target/scala-2.11/faunadb-scala_2.11-0.1-SNAPSHOT.jar` for the Scala client.

### License

All projects in this repository are licensed under the [Mozilla Public License](./LICENSE)
