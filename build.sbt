lazy val `faunadb-jvm-parent` = (project in file("."))
  .settings(Settings.publishSettings: _*)
  .settings(Settings.rootSettings)
  .aggregate(`faunadb-common`, `faunadb-scala`, `faunadb-java`)

lazy val `faunadb-common` = project.in(file("faunadb-common"))
  .settings(Settings.publishSettings: _*)
  .settings(Settings.faunadbCommonSettings)
  .settings(libraryDependencies ++= Dependencies.faunadbCommon)


lazy val `faunadb-scala` = project.in(file("faunadb-scala"))
  .dependsOn(`faunadb-common`)
  .settings(Settings.publishSettings : _*)
  .settings(Settings.faunadbScalaSettings)
  .settings(libraryDependencies ++= Dependencies.faunadbScala)

lazy val `faunadb-java` = project.in(file("faunadb-java"))
  .dependsOn(`faunadb-common`)
  .settings(Settings.publishSettings: _*)
  .settings(Settings.faunadbJavaSettings)
  .settings(libraryDependencies ++= Dependencies.faunadbJava)
