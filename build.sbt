import de.johoop.jacoco4sbt.XMLReport

val driverVersion = "1.2.7"
val asyncHttpClientVersion = "2.5.2"
val guavaVersion = "19.0"
val jacksonVersion = "2.8.8"
val jacksonDocVersion = "2.8"
val metricsVersion = "3.1.0"
val jodaTimeVersion = "2.9.4"
val jodaConvert = "1.8.1"
val scalaDefaultVersion = "2.12.2"
val scalaVersions = Seq("2.11.8", scalaDefaultVersion)

val javaDocUrl = "http://docs.oracle.com/javase/7/docs/api/"
val asyncHttpClientDocUrl = s"https://www.javadoc.io/doc/org.asynchttpclient/async-http-client/$asyncHttpClientVersion"
val guavaDocUrl = s"http://google.github.io/guava/releases/$guavaVersion/api/docs/"
val jacksonDocUrl = s"http://fasterxml.github.io/jackson-databind/javadoc/$jacksonDocVersion/"
val metricsDocUrl = s"http://dropwizard.github.io/metrics/$metricsVersion/apidocs/"
val jodaDocUrl = "http://www.joda.org/joda-time/apidocs/"
val okHttpDocUrl = "https://square.github.io/okhttp/3.x/okhttp/"

val commonApiUrl = s"http://fauna.github.io/faunadb-jvm/$driverVersion/faunadb-common/api/"
val scalaApiUrl = s"http://fauna.github.io/faunadb-jvm/$driverVersion/faunadb-scala/api/"
val javaDslApiUrl = s"http://fauna.github.io/faunadb-jvm/$driverVersion/faunadb-java-dsl/api/"
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
  pgpPublicRing := file(sys.env.getOrElse("GPG_PUBLIC_KEY", ""))
)

lazy val root = (project in file("."))
  .settings(
    name := "faunadb-jvm-parent",
    organization := "com.faunadb",
    crossPaths := false,
    autoScalaLibrary := false
  )
  .aggregate(common, scala, javaDsl, java, javaAndroid, javaShaded, androidShaded)

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
      "-link", guavaDocUrl,
      "-link", jacksonDocUrl,
      "-link", metricsDocUrl,
      "-link", asyncHttpClientDocUrl
    ),

    libraryDependencies ++= Seq(
      "org.asynchttpclient" % "async-http-client" % asyncHttpClientVersion,
      "com.google.guava" % "guava" % guavaVersion,
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
      "org.asynchttpclient" % "async-http-client" % asyncHttpClientVersion,
      "joda-time" % "joda-time" % jodaTimeVersion,
      "org.joda" % "joda-convert" % jodaConvert,
      "ch.qos.logback" % "logback-classic" % "1.1.3" % "test",
      "org.scalatest" %% "scalatest" % "3.0.3" % "test"),

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
        findDep("io.dropwizard.metrics", "metrics-core") -> url(metricsDocUrl),
        findDep("joda-time", "joda-time") -> url(jodaDocUrl))
    },

    jacoco.reportFormats in jacoco.Config := Seq(XMLReport()))

lazy val javaDsl = project.in(file("faunadb-java-dsl"))
  .settings(publishSettings: _*)
  .settings(
    name := "faunadb-java-dsl",
    crossPaths := false,
    autoScalaLibrary := false,
    exportJars := true,
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    testOptions += Tests.Argument(TestFrameworks.JUnit, "+q", "-v"),
    apiURL := Some(url(javaDslApiUrl)),

    javacOptions in (Compile, doc) := Seq("-source", "1.8",
      "-link", javaDocUrl,
      "-link", guavaDocUrl,
      "-link", jacksonDocUrl,
      "-link", metricsDocUrl,
      "-link", jodaDocUrl
    ),

    testOptions += Tests.Argument(TestFrameworks.JUnit, "-q"),
    libraryDependencies ++= Seq(
      "com.fasterxml.jackson.datatype" % "jackson-datatype-guava" % jacksonVersion,
      "joda-time" % "joda-time" % jodaTimeVersion,
      "org.joda" % "joda-convert" % jodaConvert,
      "com.google.guava" % "guava" % guavaVersion,
      "ch.qos.logback" % "logback-classic" % "1.1.3" % "test",
      "org.yaml" % "snakeyaml" % "1.14" % "test",
      "com.novocode" % "junit-interface" % "0.11" % "test",
      "org.hamcrest" % "hamcrest-library" % "1.3" % "test",
      "junit" % "junit" % "4.12" % "test"
    )
  )

lazy val java = project.in(file("faunadb-java"))
  .dependsOn(common, javaDsl % "test->test;compile->compile")
  .settings(jacoco.settings)
  .settings(publishSettings: _*)
  .settings(
    name := "faunadb-java",
    crossPaths := false,
    autoScalaLibrary := false,
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    testOptions += Tests.Argument(TestFrameworks.JUnit, "+q", "-v"),
    apiURL := Some(url(javaApiUrl)),

    javacOptions in (Compile, doc) := Seq("-source", "1.8",
      "-link", javaDocUrl,
      "-link", guavaDocUrl,
      "-link", jacksonDocUrl,
      "-link", metricsDocUrl,
      "-link", jodaDocUrl,
      "-link", asyncHttpClientDocUrl,
      "-linkoffline", commonApiUrl, "./faunadb-common/target/api",
      "-linkoffline", javaDslApiUrl, "./faunadb-java-dsl/target/api"
    ),

    testOptions += Tests.Argument(TestFrameworks.JUnit, "-q"),

    libraryDependencies ++= Seq(
      "com.fasterxml.jackson.datatype" % "jackson-datatype-guava" % jacksonVersion,
      "joda-time" % "joda-time" % jodaTimeVersion,
      "org.joda" % "joda-convert" % jodaConvert,
      "ch.qos.logback" % "logback-classic" % "1.1.3" % "test",
      "org.yaml" % "snakeyaml" % "1.14" % "test",
      "com.novocode" % "junit-interface" % "0.11" % "test",
      "org.hamcrest" % "hamcrest-library" % "1.3" % "test",
      "junit" % "junit" % "4.12" % "test"
    ),
    jacoco.reportFormats in jacoco.Config := Seq(XMLReport()),

    assemblyShadeRules in assembly := Seq(
      ShadeRule.rename("com.google.common.util.concurrent.**" -> "faunadb.@0").inAll
    ),

    assemblyOption in assembly ~= {
      _.copy(includeScala = false)
    },

    assemblyMergeStrategy in assembly := {
      case x if x.endsWith("io.netty.versions.properties") =>
        MergeStrategy.first
      case x =>
        val oldStrategy = (assemblyMergeStrategy in assembly).value
        oldStrategy(x)
    },

    logLevel in assembly := Level.Info,

    test in assembly := {}
  )

lazy val javaAndroid = project.in(file("faunadb-android"))
  .dependsOn(javaDsl % "test->test;compile->compile")
  .settings(publishSettings: _*)
  .settings(androidBuild: _*)
  .settings(
    name := "faunadb-android",
    crossPaths := false,
    autoScalaLibrary := false,
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    testOptions += Tests.Argument(TestFrameworks.JUnit, "+q", "-v"),
    apiURL := Some(url(javaAndroidApiUrl)),

    javacOptions in (Compile, doc) := Seq("-source", "1.8",
      "-link", javaDocUrl,
      "-link", guavaDocUrl,
      "-link", jacksonDocUrl,
      "-link", jodaDocUrl,
      "-link", okHttpDocUrl,
      "-linkoffline", javaDslApiUrl, "./faunadb-java-dsl/target/api"
    ),

    testOptions += Tests.Argument(TestFrameworks.JUnit, "-q"),

    libraryDependencies ++= Seq(
      "com.squareup.okhttp3" % "okhttp" % "3.4.1",
      "com.google.guava" % "guava" % guavaVersion,
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

    publishArtifact in Test := false,
    publishArtifact in Compile := true,
    publishArtifact in (Compile, packageBin) := true,
    publishArtifact in (Compile, packageDoc) := true,
    publishArtifact in (Compile, packageSrc) := true,

    proguardOptions in Android ++= Seq(
      //Guava
      "-dontwarn sun.misc.Unsafe",
      "-dontwarn java.lang.ClassValue",
      "-dontwarn java.lang.SafeVarargs",
      "-dontwarn com.google.j2objc.annotations.Weak",
      "-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement",

      //Joda Convert
      "-dontwarn org.joda.convert.**",

      //Jackson
      // We can ignore the databind package as long as we don't obfuscate the annotations package
      "-dontwarn com.fasterxml.jackson.databind.**",
      "-keep class com.fasterxml.jackson.annotation.** { *; }",
      "-keep class org.codehaus.** { *; }",

      //Okio rules
      "-dontwarn okio.**",

      //OkHttp rules
      "-keepattributes Signature",
      "-keepattributes *Annotation*",
      "-keep class okhttp3.** { *; }",
      "-keep interface okhttp3.** { *; }",
      "-dontwarn okhttp3.**",

      "-keep class com.faunadb.**"
    ),

    packagingOptions in Android := PackagingOptions(excludes = Seq(
      "META-INF/DEPENDENCIES",
      "META-INF/LICENSE.txt",
      "META-INF/LICENSE",
      "META-INF/NOTICE.txt",
      "META-INF/NOTICE"
    )),

    assemblyShadeRules in assembly := Seq(
      ShadeRule.rename("com.google.common.util.concurrent.**" -> "faunadb.@0").inAll
    ),

    assemblyOption in assembly ~= {
      _.copy(includeScala = false)
    },

    logLevel in assembly := Level.Info,

    test in assembly := {}
  )

lazy val androidShaded = project
  .settings(publishSettings: _*)
  .settings(
    name := "faunadb-android-shaded",
    crossPaths := false,
    autoScalaLibrary := false,

    packageBin in Compile := {
      (assembly in (javaAndroid, Compile)).value
    }
  )

lazy val javaShaded = project
  .settings(publishSettings: _*)
  .settings(
    name := "faunadb-java-shaded",
    crossPaths := false,
    autoScalaLibrary := false,

    packageBin in Compile := {
      (assembly in (java, Compile)).value
    }
  )

