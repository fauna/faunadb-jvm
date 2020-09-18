import Dependencies.Versions._
import com.typesafe.sbt.SbtPgp.autoImport._
import sbt.Keys._
import sbt._
import scoverage.ScoverageSbtPlugin.autoImport._

object Settings {

  lazy val driverVersion = "3.0.0"

  lazy val scala211 = "2.11.12"
  lazy val scala212 = "2.12.12"
  lazy val supportedScalaVersions = Seq(scala211, scala212)

  lazy val jacksonDocVersion = "2.10"

  lazy val javaDocUrl = "http://docs.oracle.com/javase/7/docs/api/"
  lazy val nettyClientDocUrl = "https://netty.io/4.1/api/index.html"
  lazy val jacksonDocUrl = s"http://fasterxml.github.io/jackson-databind/javadoc/$jacksonDocVersion/"
  lazy val metricsDocUrl = s"http://dropwizard.github.io/metrics/$metricsVersion/apidocs/"

  lazy val commonApiUrl = s"http://fauna.github.io/faunadb-jvm/$driverVersion/faunadb-common/api/"
  lazy val scalaApiUrl = s"http://fauna.github.io/faunadb-jvm/$driverVersion/faunadb-scala/api/"
  lazy val javaApiUrl = s"http://fauna.github.io/faunadb-jvm/$driverVersion/faunadb-java/api/"

  lazy val buildSettings = Seq(
    organization := "com.faunadb",
    version := driverVersion,
    scalaVersion := scala212
  )

  lazy val publishSettings = Seq(
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
    pgpPublicRing := file(sys.env.getOrElse("GPG_PUBLIC_KEY", ""))
  )

  lazy val javaCommonSettings = Seq(
    crossScalaVersions := Seq(scala212),
    crossPaths := false,
    autoScalaLibrary := false,

    exportJars := true,

    coverageEnabled := false,

    javacOptions ++= Seq(
      "-source", "1.8", "-target", "1.8"
    ),

    javacOptions in (Compile, doc) := Seq(
      "-source", "1.8",
      "-link", javaDocUrl,
      "-link", jacksonDocUrl,
      "-link", metricsDocUrl,
      "-link", nettyClientDocUrl
    )
  )

  lazy val commonSettings =
    buildSettings ++
    publishSettings

  lazy val rootSettings = Seq(
    // crossScalaVersions must be set to Nil on the aggregating project
    crossScalaVersions := Nil,
    publish / skip := true
  )

  lazy val faunadbCommonSettings = Seq(
    apiURL := Some(url(commonApiUrl))
  )

  lazy val faunadbJavaSettings = Seq(
    apiURL := Some(url(javaApiUrl)),

    javacOptions in (Compile, doc) ++= Seq(
      "-linkoffline", commonApiUrl, "./faunadb-common/target/api"
    ),

    testOptions += Tests.Argument(TestFrameworks.JUnit, "+q", "-v")
  )

  lazy val faunadbScalaSettings = Seq(
    crossScalaVersions := supportedScalaVersions,

    scalacOptions ++= Seq(
      "-Xsource:2.12",
      "-Xmax-classfile-name", "240"
    ),

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
    }
  )

}
