import de.johoop.jacoco4sbt.XMLReport

val driverVersion = "2.6.0-SNAPSHOT"
val nettyVersion = "4.1.31.Final"
val jacksonVersion = "2.8.8"
val jacksonDocVersion = "2.8"
val metricsVersion = "3.1.0"
val scalaDefaultVersion = "2.12.2"
val scalaVersions = Seq("2.11.8", scalaDefaultVersion)

val javaDocUrl = "http://docs.oracle.com/javase/7/docs/api/"
val nettyClientDocUrl = "https://netty.io/4.1/api/index.html"
val jacksonDocUrl = s"http://fasterxml.github.io/jackson-databind/javadoc/$jacksonDocVersion/"
val metricsDocUrl = s"http://dropwizard.github.io/metrics/$metricsVersion/apidocs/"

val commonApiUrl = s"http://fauna.github.io/faunadb-jvm/$driverVersion/faunadb-common/api/"
val scalaApiUrl = s"http://fauna.github.io/faunadb-jvm/$driverVersion/faunadb-scala/api/"
val javaApiUrl = s"http://fauna.github.io/faunadb-jvm/$driverVersion/faunadb-java/api/"
val javaAndroidApiUrl = s"http://fauna.github.io/faunadb-jvm/$driverVersion/faunadb-android/api/"

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
      <url>git@github.com:fauna/faunadb-jvm.git</url>
      <connection>scm:git:git@github.com:fauna/faunadb-jvm.git</connection>
    </scm>
    <developers>
      <developer>
        <name>Matt Freels</name>
        <email>matt@fauna.com</email>
        <organization>Fauna</organization>
        <organizationUrl>http://fauna.com</organizationUrl>
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
  .aggregate(common, scala, java, javaAndroid)

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
      "org.slf4j" % "slf4j-api" % "1.7.7",
      "com.fasterxml.jackson.core" % "jackson-core" % jacksonVersion,
      "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion
    )
  )

crossScalaVersions := scalaVersions

lazy val scala = project.in(file("faunadb-scala"))
  .dependsOn(common)
  .settings(jacoco.settings)
  .settings(publishSettings : _*)
  .settings(
    name := "faunadb-scala",
    scalaVersion := scalaDefaultVersion,

    libraryDependencies ++= Seq(
      "com.fasterxml.jackson.core" % "jackson-annotations" % jacksonVersion,
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion,
      "ch.qos.logback" % "logback-classic" % "1.1.3" % "test",
      "org.scala-lang.modules" % "scala-java8-compat_2.12" % "0.9.0",
      "org.scalatest" %% "scalatest" % "3.0.3" % "test"
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

    libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.1.3" % "test",
      "org.yaml" % "snakeyaml" % "1.14" % "test",
      "com.novocode" % "junit-interface" % "0.11" % "test",
      "org.hamcrest" % "hamcrest-library" % "1.3" % "test",
      "junit" % "junit" % "4.12" % "test"
    ),
    jacoco.reportFormats in jacoco.Config := Seq(XMLReport())
  )

// This project will not publish any artifacts, the java artifact `faunadb-java` should be used as a regular dependency
// The sole purpose of it, it's to compile against the android-sdk
lazy val javaAndroid = project.in(file("faunadb-java"))
  .enablePlugins(AndroidApp)
  .dependsOn(common)
  .settings(
    name := "faunadb-android",
    crossPaths := false,
    autoScalaLibrary := false,
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),

    testOptions += Tests.Argument(TestFrameworks.JUnit, "+q", "-v"),

    target := { baseDirectory.value / "target-android" },
    javacOptions in (Compile, doc) := Seq("-source", "1.8"),
    testOptions += Tests.Argument(TestFrameworks.JUnit, "-q"),

    retrolambdaEnabled := true,

    libraryDependencies ++= Seq(
      "com.google.code.findbugs" % "jsr305" % "2.0.1",
      "com.novocode" % "junit-interface" % "0.11" % "test",
      "org.hamcrest" % "hamcrest-library" % "1.3" % "test",
      "junit" % "junit" % "4.12" % "test"
    ),

    platformTarget in Android := "android-26",
    buildToolsVersion in Android := Some("26.0.0"),
    minSdkVersion in Android := "26",
    showSdkProgress in Android := true,
    useProguard := true,
    useProguardInDebug := true,
    publishArtifact := false,

    proguardOptions in Android ++= Seq(
      // We can ignore those warnings from all the 3rd party libraries

      //used by retrolambda
      "-dontwarn java.lang.invoke.**",

      // used by com.fasterxml.jackson.datatype
      "-dontwarn org.w3c.dom.**",
      "-dontwarn java.beans.**",

      //used by io.netty
      "-dontwarn com.google.protobuf.**",
      "-dontwarn com.jcraft.**",
      "-dontwarn com.ning.compress.**",
      "-dontwarn lzma.sdk.**",
      "-dontwarn net.jpountz.**",
      "-dontwarn org.apache.commons.logging.**",
      "-dontwarn org.apache.log4j.**",
      "-dontwarn org.apache.logging.log4j.**",
      "-dontwarn org.bouncycastle.**",
      "-dontwarn org.conscrypt.**",
      "-dontwarn org.eclipse.jetty.**",
      "-dontwarn org.jboss.marshalling.**",
      "-dontwarn sun.security.**",
      // internals
      "-dontwarn io.netty.internal.tcnative.**",
      "-dontwarn io.netty.util.internal.logging.**",

      //used by io.dropwizard.metrics
      "-dontwarn javax.management.**",
      "-dontwarn java.lang.management.**",
      "-dontwarn sun.misc.Unsafe",

      //used by slf4j
      "-dontwarn org.slf4j.impl.StaticLoggerBinder",
      "-dontwarn org.slf4j.impl.StaticMDCBinder",
      "-dontwarn org.slf4j.impl.StaticMarkerBinder",

      "-keep class com.faunadb.**"
    ),

    packagingOptions in Android := PackagingOptions(excludes = Seq(
      "META-INF/DEPENDENCIES",
      "META-INF/LICENSE.txt",
      "META-INF/LICENSE",
      "META-INF/NOTICE.txt",
      "META-INF/NOTICE",
      "META-INF/io.netty.versions.properties",
      "META-INF/INDEX.LIST"
    ))
  )

