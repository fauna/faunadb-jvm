val jacksonVersion = "2.5.1"
val metricsVersion = "3.1.0"

lazy val commonSettings = Seq(
  version := "0.1-SNAPSHOT"
)

lazy val root = (project in file("."))
  .settings(commonSettings: _*)
  .settings(
    name := "faunadb-jvm",
    libraryDependencies ++= Seq(
      "commons-beanutils" % "commons-beanutils" % "1.9.2"
    )
   ).aggregate(httpclient, scala)


lazy val httpclient = project.in(file("faunadb-httpclient"))
  .settings(commonSettings: _*)
  .settings(
    name := "faunadb-httpclient",
    crossPaths := false,
    autoScalaLibrary := false,
    compileOrder := CompileOrder.JavaThenScala,
    javacOptions ++= Seq("-source", "1.7", "-target", "1.7"),
    libraryDependencies ++= Seq(
      "com.ning" % "async-http-client" % "1.8.15",
      "com.google.guava" % "guava" % "18.0",
      "io.dropwizard.metrics" % "metrics-core" % metricsVersion,
      "org.slf4j" % "slf4j-api" % "1.7.7",
      "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion,
      "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion
    )
  )

lazy val scala = project.in(file("faunadb-scala"))
  .settings(commonSettings : _*)
  .settings(
    name := "faunadb-scala",
    scalaVersion := "2.11.6",
    libraryDependencies ++= Seq(
      "com.fasterxml.jackson.core" % "jackson-annotations" % jacksonVersion,
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion,
      "org.yaml" % "snakeyaml" % "1.14" % "test",
      "org.scalatest" %% "scalatest" % "2.2.1" % "test"
    )
  )
  .dependsOn(httpclient)

lazy val java = project.in(file("faunadb-java"))
  .settings(commonSettings: _*)
  .settings(
    name := "faunadb-java",
    crossPaths := false,
    autoScalaLibrary := false,
    compileOrder := CompileOrder.JavaThenScala,
    javacOptions ++= Seq("-source", "1.7", "-target", "1.7"),
    libraryDependencies ++= Seq(
      "junit" % "junit" % "4.12" % "test"
    )
  )
  .dependsOn(httpclient)
