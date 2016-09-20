# FaunaDB JVM Drivers

[![Build Status](https://img.shields.io/travis/faunadb/faunadb-jvm/master.svg?maxAge=21600)](https://travis-ci.org/faunadb/faunadb-jvm)
[![Coverage Status](https://img.shields.io/codecov/c/github/faunadb/faunadb-jvm/master.svg?maxAge=21600)](https://codecov.io/gh/faunadb/faunadb-jvm/branch/master)
[![Maven Central](https://img.shields.io/maven-central/v/com.faunadb/faunadb-common.svg?maxAge=21600)](https://search.maven.org/#search%7Cga%7C1%7Cg%3A%22com.faunadb%22)
[![License](https://img.shields.io/badge/license-MPL_2.0-blue.svg?maxAge=2592000)](https://raw.githubusercontent.com/faunadb/faunadb-jvm/master/LICENSE)

This repository contains the FaunaDB drivers for the JVM languages. Currently, Java, Android and Scala clients are implemented.

### Features

* All drivers fully support the current version of the [FaunaDB API](https://faunadb.com/documentation).
* Java and Scala clients share the same underlying library [faunadb-common](./faunadb-common).
* Java and Android clients share the same dsl library [faunadb-java-dsl](./faunadb-java-dsl).
* Supports [Dropwizard Metrics](https://dropwizard.github.io/metrics/3.1.0/) hooks for stats reporting (except Android).
* Support Android 4.1 (API level 16)

## Installation

Download from the Maven central repository:

### faunadb-java/pom.xml:

```xml
  <dependencies>
  ...
  <dependency>
    <groupId>com.faunadb</groupId>
    <artifactId>faunadb-java</artifactId>
    <version>0.3.3</version>
    <scope>compile</scope>
  </dependency>
  ...
</dependencies>
```

### faunadb-android/pom.xml:

```xml
  <dependencies>
  ...
  <dependency>
    <groupId>com.faunadb</groupId>
    <artifactId>faunadb-android</artifactId>
    <version>0.3.3</version>
    <scope>compile</scope>
  </dependency>
  ...
</dependencies>
```
### faunadb-scala/sbt

```scala
libraryDependencies += ("com.faunadb" %% "faunadb-scala" % "0.3.3")
```

## Documentation

Javadocs and Scaladocs are hosted on GitHub:

* [faunadb-java](http://faunadb.github.io/faunadb-jvm/0.3.3/faunadb-java/api/)
* [faunadb-android](http://faunadb.github.io/faunadb-jvm/0.3.3/faunadb-android/api/)
* [faunadb-scala](http://faunadb.github.io/faunadb-jvm/0.3.3/faunadb-scala/api/)

## Dependencies

### Shared

* [Jackson](https://github.com/FasterXML/jackson) for JSON parsing.
* [Async HTTP client](https://github.com/AsyncHttpClient/async-http-client) and [Netty](http://netty.io/) for the HTTP transport.
* [Joda Time](http://www.joda.org/joda-time/) for date and time manipulation.

### Android

* [OkHttp client](http://square.github.io/okhttp/) for the HTTP transport.

### Java

* Java 7
* [Google Guava](https://github.com/google/guava), for collections and ListenableFutures.

### Scala

* Scala 2.11.x

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
