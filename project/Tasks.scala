import sbt.Keys._
import sbt.{TaskKey, _}

object Tasks {

  lazy val compileAll = TaskKey[Unit]("compile-all")
  
  lazy val settings =
    Seq(
      compileAll := (compile in Testing.LoadTest)
        .dependsOn(compile in Test)
        .dependsOn(compile in Compile)
        .value)

}
