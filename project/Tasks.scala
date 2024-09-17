import sbt.Keys._
import sbt.{TaskKey, _}

object Tasks {

  lazy val compileAll = TaskKey[Unit]("compile-all")
  
  lazy val settings = Seq.empty[Def.Setting[Task[Unit]]]
}
