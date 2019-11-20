val driverVersion = "2.9.0"
val nettyVersion = "4.1.36.Final"
val jacksonVersion = "2.8.11"
val jacksonDocVersion = "2.8"
val metricsVersion = "4.1.0"
val scalaDefaultVersion = "2.12.8"
val scalaVersions = Seq("2.11.12", scalaDefaultVersion)

val javaDocUrl = "http://docs.oracle.com/javase/7/docs/api/"
val nettyClientDocUrl = "https://netty.io/4.1/api/index.html"
val jacksonDocUrl = s"http://fasterxml.github.io/jackson-databind/javadoc/$jacksonDocVersion/"
val metricsDocUrl = s"http://dropwizard.github.io/metrics/$metricsVersion/apidocs/"

val commonApiUrl = s"http://fauna.github.io/faunadb-jvm/$driverVersion/faunadb-common/api/"
val scalaApiUrl = s"http://fauna.github.io/faunadb-jvm/$driverVersion/faunadb-scala/api/"
val javaApiUrl = s"http://fauna.github.io/faunadb-jvm/$driverVersion/faunadb-java/api/"

lazy val publishSettings = Seq(
  version := driverVersion,
  organization := "com.faunadb",
  licenses := Seq("Mozilla Public License" -> url("https://www.mozilla.org/en-US/MPL/2.0/")),
  homepage := Some(url("https://github.com/fauna/faunadb-jvm")),
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
  credentials += Credentials(
    "Sonatype Nexus Repository Manager",
    "oss.sonatype.org",
    sys.env.getOrElse("SONATYPE_USER", ""),
    sys.env.getOrElse("SONATYPE_PASS", "")
  ),
  pomExtra := (
    <scm>
      <url>https://github.com/fauna/faunadb-jvm</url>
      <connection>scm:git:git@github.com:fauna/faunadb-jvm.git</connection>
    </scm>
    <developers>
      <developer>
        <name>Engineering</name>
        <email>production@fauna.com</email>
        <organization>Fauna, Inc</organization>
        <organizationUrl>https://fauna.com</organizationUrl>
      </developer>
    </developers>
  ),
  usePgpKeyHex(sys.env.getOrElse("GPG_SIGNING_KEY", "0")),
  pgpPassphrase := sys.env.get("GPG_PASSPHRASE") map (_.toArray),
  pgpSecretRing := file(sys.env.getOrElse("GPG_PRIVATE_KEY", "")),
  pgpPublicRing := file(sys.env.getOrElse("GPG_PUBLIC_KEY", "")))

lazy val root = (project in file("."))
  .settings(publishSettings: _*)
  .settings(
    name := "faunadb-jvm-parent",
    crossPaths := false,
    autoScalaLibrary := false
  )
  .aggregate(common, scala, java)

lazy val common = project.in(file("faunadb-common"))
  .settings(publishSettings: _*)
  .settings(
    name := "faunadb-common",
    crossPaths := false,
    autoScalaLibrary := false,
    exportJars := true,
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    apiURL := Some(url(commonApiUrl)),

    javacOptions in (Compile, doc) := Seq("-source", "1.8",
      "-link", javaDocUrl,
      "-link", jacksonDocUrl,
      "-link", metricsDocUrl,
      "-link", nettyClientDocUrl
    ),

    libraryDependencies ++= Seq(
      "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % jacksonVersion,
      "io.netty" % "netty-codec-http" % nettyVersion,
      "io.netty" % "netty-handler" % nettyVersion,
      "io.dropwizard.metrics" % "metrics-core" % metricsVersion,
      "org.slf4j" % "slf4j-api" % "1.7.26",
      "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion,
      "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion
    )
  )

lazy val scala = project.in(file("faunadb-scala"))
  .dependsOn(common)
  .settings(publishSettings : _*)
  .settings(
    name := "faunadb-scala",
    scalaVersion := scalaDefaultVersion,
    crossScalaVersions := scalaVersions,
    
    scalacOptions ++= Seq(
      "-Xsource:2.12"
    ),

    libraryDependencies ++= Seq(
      "com.fasterxml.jackson.core" % "jackson-annotations" % jacksonVersion,
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion,
      "ch.qos.logback" % "logback-classic" % "1.2.3" % "test",
      "org.scala-lang.modules" % "scala-java8-compat_2.12" % "0.9.0",
      "org.scalatest" %% "scalatest" % "3.0.7" % "test"
    ),

    coverageEnabled := !scalaVersion.value.startsWith("2.11"),

    autoAPIMappings := true,
    apiURL := Some(url(scalaApiUrl)),
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
        findDep("com.fasterxml.jackson.core", "jackson-databind") -> url(jacksonDocUrl),
        findDep("io.dropwizard.metrics", "metrics-core") -> url(metricsDocUrl))
    })

lazy val java = project.in(file("faunadb-java"))
  .dependsOn(common)
  .settings(publishSettings: _*)
  .settings(
    name := "faunadb-java",
    crossPaths := false,
    autoScalaLibrary := false,
    exportJars := true,
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    testOptions += Tests.Argument(TestFrameworks.JUnit, "+q", "-v"),
    apiURL := Some(url(javaApiUrl)),

    javacOptions in (Compile, doc) := Seq("-source", "1.8",
      "-link", javaDocUrl,
      "-link", jacksonDocUrl,
      "-link", metricsDocUrl,
      "-link", nettyClientDocUrl,
      "-linkoffline", commonApiUrl, "./faunadb-common/target/api"
    ),

    testOptions += Tests.Argument(TestFrameworks.JUnit, "-q"),
    coverageEnabled := true,

    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.2.3" % "test",
      "org.yaml" % "snakeyaml" % "1.24" % "test",
      "com.novocode" % "junit-interface" % "0.11" % "test",
      "org.hamcrest" % "hamcrest-library" % "2.1" % "test",
      "junit" % "junit" % "4.12" % "test"
    ))
