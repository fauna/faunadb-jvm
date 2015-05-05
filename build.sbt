name := "faunadb-java"

version := "0.1-SNAPSHOT"

scalaVersion := "2.11.6"

val jacksonVersion = "2.5.1"
val metricsVersion = "3.1.0"

libraryDependencies ++= Seq(
  "com.ning" % "async-http-client" % "1.9.15",
  "com.google.guava" % "guava" % "18.0",
  "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-annotations" % jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion,
  "io.dropwizard.metrics" % "metrics-core" % metricsVersion,
  "commons-beanutils" % "commons-beanutils" % "1.9.2",
  "org.slf4j" % "slf4j-api" % "1.7.7",
  "org.yaml" % "snakeyaml" % "1.14" % "test",
  "org.scalatest" %% "scalatest" % "2.2.1" % "test"
)
