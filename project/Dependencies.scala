import sbt._

object Dependencies {

  object Versions {
    val jacksonVersion               = "2.12.1"
    val metricsVersion               = "4.1.12.1"
    val slf4jVersion                 = "1.7.30"
    val logbackVersion               = "1.2.3"
    val scalaTestVersion             = "3.2.3"
    val scalaJava8CompatVersion      = "0.9.1"
    val scalaReflectScala211Version  = "2.11.12"
    val scalaReflectScala212Version  = "2.12.14"
    val snakeYamlVersion             = "1.27"
    val junitInterfaceVersion        = "0.11"
    val harmcrestLibraryVersion      = "2.2"
    val junitVersion                 = "4.13.2"
    val reactiveStreamsVersion       = "1.0.3"
    val monixVersion                 = "3.3.0"
    val enumeratumVersion            = "1.7.0"
  }

  // Projects
  val faunadbCommon = Seq.empty[ModuleID]
  val faunadbJava = Seq.empty[ModuleID]
  def faunadbScala(scalaVersion: String) = Seq.empty[ModuleID]
}
