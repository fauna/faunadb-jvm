lazy val `faunadb-jvm` =
  (project in file("."))
    .settings(Settings.publishSettings: _*)
    .settings(Settings.rootSettings)
    .aggregate(`faunadb-common`, `faunadb-scala`, `faunadb-java`)

lazy val `faunadb-common` =
  project
    .settings(Settings.publishSettings: _*)
    .settings(Settings.faunadbCommonSettings)
    .settings(libraryDependencies ++= Dependencies.faunadbCommon)

lazy val `faunadb-java` =
  project
    .dependsOn(`faunadb-common`)
    .settings(Settings.publishSettings: _*)
    .settings(Settings.faunadbJavaSettings)
    .settings(libraryDependencies ++= Dependencies.faunadbJava)

lazy val `faunadb-scala` =
  project
    .dependsOn(`faunadb-common`)
    .settings(Settings.publishSettings : _*)
    .settings(Settings.faunadbScalaSettings)
    .settings(libraryDependencies ++= Dependencies.faunadbScala)