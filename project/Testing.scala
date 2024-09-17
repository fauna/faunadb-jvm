import sbt._

object Testing {
  // Configs
  lazy val LoadTest = config("load") extend Test

  // Tasks
  lazy val testAll = TaskKey[Unit]("test-all")

  // Settings
  lazy val loadTestSettings =
    inConfig(LoadTest)(
      Defaults.testSettings
    )

  lazy val settings =
    loadTestSettings
}