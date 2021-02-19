lazy val `faunadb-jvm` =
  (project in file("."))
    .settings(Settings.commonSettings: _*)
    .settings(Settings.rootSettings: _*)
    .aggregate(`faunadb-common`, `faunadb-java`, `faunadb-scala`)

lazy val `faunadb-common` =
  project
    .enablePlugins(BuildInfoPlugin)
    .configs(Configs.commonConfigs: _*)
    .settings(Settings.commonSettings: _*)
    .settings(Settings.javaCommonSettings: _*)
    .settings(Settings.faunadbCommonSettings)
    .settings(libraryDependencies ++= Dependencies.faunadbCommon(scalaVersion.value))

lazy val `faunadb-java` =
  project
    .dependsOn(`faunadb-common`)
    .configs(Configs.commonConfigs: _*)
    .settings(Settings.commonSettings: _*)
    .settings(Settings.javaCommonSettings: _*)
    .settings(Settings.faunadbJavaSettings)
    .settings(libraryDependencies ++= Dependencies.faunadbJava)

lazy val `faunadb-scala` =
  project
    .dependsOn(`faunadb-common`)
    .configs(Configs.commonConfigs: _*)
    .settings(Settings.commonSettings: _*)
    .settings(Settings.faunadbScalaSettings)
    .settings(libraryDependencies ++= Dependencies.faunadbScala(scalaVersion.value))