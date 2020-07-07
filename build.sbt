lazy val `faunadb-jvm` =
  (project in file("."))
    .settings(Settings.faunadbJvmSettings)
    .aggregate(`faunadb-common`, `faunadb-java`, `faunadb-scala`)

lazy val `faunadb-common` =
  project
    .settings(Settings.commonSettings: _*)
    .settings(Settings.javaCommonSettings: _*)
    .settings(Settings.faunadbCommonSettings)
    .settings(libraryDependencies ++= Dependencies.faunadbCommon)

lazy val `faunadb-java` =
  project
    .dependsOn(`faunadb-common`)
    .settings(Settings.commonSettings: _*)
    .settings(Settings.javaCommonSettings: _*)
    .settings(Settings.faunadbJavaSettings)
    .settings(libraryDependencies ++= Dependencies.faunadbJava)

lazy val `faunadb-scala` =
  project
    .dependsOn(`faunadb-common`)
    .settings(Settings.commonSettings: _*)
    .settings(Settings.faunadbScalaSettings)
    .settings(libraryDependencies ++= Dependencies.faunadbScala(scalaVersion.value))