val jacksonVersion = "2.6.4"
val metricsVersion = "3.1.0"
val baseScalaVersion = "2.11.8"

lazy val publishSettings = Seq(
  version := "0.2.2-SNAPSHOT",
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
    </scm>))

lazy val root = (project in file("."))
  .settings(
    name := "faunadb-jvm-parent",
    crossPaths := false,
    autoScalaLibrary := false)
  .aggregate(common, scala, java)

lazy val common = project.in(file("faunadb-common"))
  .settings(publishSettings: _*)
  .settings(
    name := "faunadb-common",
    crossPaths := false,
    autoScalaLibrary := false,
    javacOptions ++= Seq("-source", "1.7", "-target", "1.7"),
    apiURL := Some(url("http://faunadb.github.io/faunadb-jvm/faunadb-common/api/")),

    (javacOptions in doc) := Seq("-source", "1.7",
      "-link", "http://docs.oracle.com/javase/7/docs/api/",
      "-link", "http://docs.guava-libraries.googlecode.com/git-history/v18.0/javadoc/",
      "-link", "http://fasterxml.github.io/jackson-databind/javadoc/2.5/",
      "-link", "https://dropwizard.github.io/metrics/3.1.0/apidocs/",
      "-linkoffline", "http://static.javadoc.io/com.ning/async-http-client/1.9.32", "./faunadb-common/doc/com.ning/async-http-client/1.9.32"),

    libraryDependencies ++= Seq(
      "com.ning" % "async-http-client" % "1.9.32",
      "com.google.guava" % "guava" % "19.0",
      "io.dropwizard.metrics" % "metrics-core" % metricsVersion,
      "org.slf4j" % "slf4j-api" % "1.7.7",
      "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion,
      "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion))

lazy val scala = project.in(file("faunadb-scala"))
  .dependsOn(common)
  .settings(publishSettings : _*)
  .settings(
    name := "faunadb-scala",
    scalaVersion := baseScalaVersion,

    libraryDependencies ++= Seq(
      "com.fasterxml.jackson.core" % "jackson-annotations" % jacksonVersion,
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion,
      "ch.qos.logback" % "logback-classic" % "1.1.3" % "test",
      "org.scalatest" %% "scalatest" % "2.2.1" % "test"),

    autoAPIMappings := true,
    apiURL := Some(url("http://faunadb.github.io/faunadb-jvm/faunadb-scala/api/")),
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
        findDep("com.fasterxml.jackson.core", "jackson-databind") ->
          url("http://fasterxml.github.io/jackson-databind/javadoc/2.5/"),
        findDep("io.dropwizard.metrics", "metrics-core") ->
          url(s"https://dropwizard.github.io/metrics/${metricsVersion}/apidocs/"))
    })

lazy val java = project.in(file("faunadb-java"))
  .dependsOn(common)
  .settings(publishSettings: _*)
  .settings(
    name := "faunadb-java",
    crossPaths := false,
    autoScalaLibrary := false,
    javacOptions ++= Seq("-source", "1.7", "-target", "1.7"),
    testOptions += Tests.Argument(TestFrameworks.JUnit, "+q", "-v"),
    apiURL := Some(url("http://faunadb.github.io/faunadb-jvm/faunadb-java/api/")),

    (javacOptions in doc) := Seq("-source", "1.7",
      "-link", "http://docs.oracle.com/javase/7/docs/api/",
      "-link", "http://docs.guava-libraries.googlecode.com/git-history/v18.0/javadoc/",
      "-link", "http://fasterxml.github.io/jackson-databind/javadoc/2.5/",
      "-link", ((target in common).value / "api").toString),

    testOptions += Tests.Argument(TestFrameworks.JUnit, "-q"),

    libraryDependencies ++= Seq(
      "com.fasterxml.jackson.datatype" % "jackson-datatype-guava" % jacksonVersion,
      "ch.qos.logback" % "logback-classic" % "1.1.3" % "test",
      "org.apache.commons" % "commons-lang3" % "3.4" % "test",
      "org.yaml" % "snakeyaml" % "1.14" % "test",
      "com.novocode" % "junit-interface" % "0.11" % "test",
      "org.hamcrest" % "hamcrest-library" % "1.3" % "test",
      "junit" % "junit" % "4.12" % "test"))
