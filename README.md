# FaunaDB JVM Clients

This repository contains the FaunaDB clients for the JVM languages. Currently, Java and Scala clients are implemented.

### Features

* All clients fully support the current version of the [FaunaDB API](https://faunadb.com/documentation).
* All per-language clients share the same underlying library [faunadb-common](./faunadb-common).
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

TBD

### License

All projects in this repository are licensed under the [Mozilla Public License](./LICENSE)
