import sbt.Keys._
import sbt.{TaskKey, _}

object Tasks {

  lazy val compileAll = TaskKey[Unit]("compile-all")
  
  lazy val settings =
    Seq(
      compileAll := (Testing.LoadTest / compile)
        .dependsOn(Test / compile)
        .dependsOn(Compile / compile)
        .value)

}
