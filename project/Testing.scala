import sbt.Keys.{baseDirectory, scalaSource, _}
import sbt._

object Testing {
  // Configs
  lazy val LoadTest = config("load") extend Test

  // Tasks
  lazy val testAll = TaskKey[Unit]("test-all")

  // Settings
  lazy val loadTestSettings =
    inConfig(LoadTest)(
      Defaults.testSettings ++
      Seq(
        fork in LoadTest := true,
        parallelExecution in LoadTest := false,
        scalaSource in LoadTest := baseDirectory.value / "src/load/scala",
        javaSource in LoadTest := baseDirectory.value / "src/load/java")
    )

  lazy val settings =
    loadTestSettings ++
    Seq(testAll := (test in LoadTest).dependsOn(test in Test).value)

}

