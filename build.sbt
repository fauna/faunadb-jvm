import de.johoop.jacoco4sbt.XMLReport

val driverVersion = "0.3.3"
val asyncHttpClientVersion = "2.0.2"
val guavaVersion = "19.0"
val jacksonVersion = "2.6.4"
val jacksonDocVersion = "2.6"
val metricsVersion = "3.1.0"
val jodaTimeVersion = "2.9.4"
val baseScalaVersion = "2.11.8"

lazy val publishSettings = Seq(
  version := driverVersion,
  organization := "com.faunadb",
  licenses := Seq("Mozilla Public License" -> url("https://www.mozilla.org/en-US/MPL/2.0/")),
  homepage := Some(url("https://github.com/faunadb/faunadb-jvm")),
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  publishTo := {
    val nexus = "https://oss.sonatype.org"
    if (isSnapshot.value) {
      Some("Snapshots" at s"$nexus/content/repositories/snapshots")
    } else {
      Some("Releases" at s"$nexus/service/local/staging/deploy/maven2")
    }
  },
  pomExtra := (
    <scm>
      <url>git@github.com:faunadb/faunadb-jvm.git</url>
      <connection>scm:git:git@github.com:faunadb/faunadb-jvm.git</connection>
    </scm>
    <developers>
      <developer>
        <name>Matt Freels</name>
        <email>matt@fauna.com</email>
        <organization>Fauna</organization>
        <organizationUrl>http://fauna.com</organizationUrl>
      </developer>
    </developers>
  ))

lazy val root = (project in file("."))
  .settings(
    name := "faunadb-jvm-parent",
    organization := "com.faunadb",
    crossPaths := false,
    autoScalaLibrary := false)
  .aggregate(common, scala, java)

lazy val common = project.in(file("faunadb-common"))
  .settings(publishSettings: _*)
  .settings(
    name := "faunadb-common",
    crossPaths := false,
    autoScalaLibrary := false,
    exportJars := true,
    javacOptions ++= Seq("-source", "1.7", "-target", "1.7"),
    apiURL := Some(url(s"http://faunadb.github.io/faunadb-jvm/$driverVersion/faunadb-common/api/")),

    (javacOptions in doc) := Seq("-source", "1.7", "-Xdoclint:none",
      "-link", "http://docs.oracle.com/javase/7/docs/api/",
      "-link", s"http://google.github.io/guava/releases/$guavaVersion/api/docs/",
      "-link", s"http://fasterxml.github.io/jackson-databind/javadoc/$jacksonDocVersion/",
      "-link", s"http://dropwizard.github.io/metrics/$metricsVersion/apidocs/",
      "-linkoffline", s"http://static.javadoc.io/org.asynchttpclient/async-http-client/$asyncHttpClientVersion/", s"./faunadb-common/doc/org.asynchttpclient/async-http-client/$asyncHttpClientVersion/"),

    libraryDependencies ++= Seq(
      "org.asynchttpclient" % "async-http-client" % asyncHttpClientVersion,
      "com.google.guava" % "guava" % guavaVersion,
      "io.dropwizard.metrics" % "metrics-core" % metricsVersion,
      "org.slf4j" % "slf4j-api" % "1.7.7",
      "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion,
      "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion))

lazy val scala = project.in(file("faunadb-scala"))
  .dependsOn(common)
  .settings(jacoco.settings)
  .settings(publishSettings : _*)
  .settings(
    name := "faunadb-scala",
    scalaVersion := baseScalaVersion,

    libraryDependencies ++= Seq(
      "com.fasterxml.jackson.core" % "jackson-annotations" % jacksonVersion,
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion,
      "joda-time" % "joda-time" % jodaTimeVersion,
      "ch.qos.logback" % "logback-classic" % "1.1.3" % "test",
      "org.scalatest" %% "scalatest" % "2.2.1" % "test"),

    autoAPIMappings := true,
    apiURL := Some(url(s"http://faunadb.github.io/faunadb-jvm/$driverVersion/faunadb-scala/api/")),
    apiMappings ++= {
      val cp = (fullClasspath in Compile).value
      def findDep(org: String, name: String) = {
        for {
          entry <- cp
          module <- entry.get(moduleID.key)
          if module.organization == org
          if module.name.startsWith(name)
          jarFile = entry.data
        } yield jarFile
      }.head

      Map(
        findDep("org.asynchttpclient", "async-http-client") ->
          url(s"http://static.javadoc.io/org.asynchttpclient/async-http-client/$asyncHttpClientVersion/"),
        findDep("com.fasterxml.jackson.core", "jackson-databind") ->
          url(s"http://fasterxml.github.io/jackson-databind/javadoc/$jacksonDocVersion/"),
        findDep("io.dropwizard.metrics", "metrics-core") ->
          url(s"http://dropwizard.github.io/metrics/$metricsVersion/apidocs/"),
        findDep("joda-time", "joda-time") ->
          url("http://www.joda.org/joda-time/apidocs/index.html"))
    },

    jacoco.reportFormats in jacoco.Config := Seq(XMLReport()))

lazy val java = project.in(file("faunadb-java"))
  .dependsOn(common)
  .settings(jacoco.settings)
  .settings(publishSettings: _*)
  .settings(
    name := "faunadb-java",
    crossPaths := false,
    autoScalaLibrary := false,
    javacOptions ++= Seq("-source", "1.7", "-target", "1.7"),
    testOptions += Tests.Argument(TestFrameworks.JUnit, "+q", "-v"),
    apiURL := Some(url("http://faunadb.github.io/faunadb-jvm/faunadb-java/api/")),

    (javacOptions in doc) := Seq("-source", "1.7", "-Xdoclint:none",
      "-link", "http://docs.oracle.com/javase/7/docs/api/",
      "-link", s"http://google.github.io/guava/releases/$guavaVersion/api/docs/",
      "-link", s"http://fasterxml.github.io/jackson-databind/javadoc/$jacksonDocVersion/",
      "-link", s"http://faunadb.github.io/faunadb-jvm/$driverVersion/faunadb-common/api/",
      "-link", s"http://dropwizard.github.io/metrics/$metricsVersion/apidocs/",
      "-link", "http://www.joda.org/joda-time/apidocs/",
      "-linkoffline", s"http://static.javadoc.io/org.asynchttpclient/async-http-client/$asyncHttpClientVersion", s"./faunadb-common/doc/org.asynchttpclient/async-http-client/$asyncHttpClientVersion"),

    testOptions += Tests.Argument(TestFrameworks.JUnit, "-q"),

    libraryDependencies ++= Seq(
      "com.fasterxml.jackson.datatype" % "jackson-datatype-guava" % jacksonVersion,
      "joda-time" % "joda-time" % jodaTimeVersion,
      "ch.qos.logback" % "logback-classic" % "1.1.3" % "test",
      "org.yaml" % "snakeyaml" % "1.14" % "test",
      "com.novocode" % "junit-interface" % "0.11" % "test",
      "org.hamcrest" % "hamcrest-library" % "1.3" % "test",
      "junit" % "junit" % "4.12" % "test"),

    jacoco.reportFormats in jacoco.Config := Seq(XMLReport()))
