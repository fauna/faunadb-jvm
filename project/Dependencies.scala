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
  }

  // Libraries
  object Compile {
    import Versions._

    // faunadb-common
    val jacksonCore           = "com.fasterxml.jackson.core"      %  "jackson-core"             % jacksonVersion
    val jacksonDatabind       = "com.fasterxml.jackson.core"      %  "jackson-databind"         % jacksonVersion
    val jacksonDatatype       = "com.fasterxml.jackson.datatype"  %  "jackson-datatype-jdk8"    % jacksonVersion

    val metrics               = "io.dropwizard.metrics"           %  "metrics-core"             % metricsVersion
    val slf4j                 = "org.slf4j"                       %  "slf4j-api"                % slf4jVersion

    // faunadb-scala
    val jacksonModuleScala    = "com.fasterxml.jackson.module"    %% "jackson-module-scala"     % jacksonVersion
    val jacksonAnnotations    = "com.fasterxml.jackson.core"      %  "jackson-annotations"      % jacksonVersion
    val scalaJava8Compat      = "org.scala-lang.modules"          %% "scala-java8-compat"       % scalaJava8CompatVersion
    val scalaReflectScala211  = "org.scala-lang"                  %  "scala-reflect"            % scalaReflectScala211Version
    val scalaReflectScala212  = "org.scala-lang"                  %  "scala-reflect"            % scalaReflectScala212Version
  }

  object Test {
    import Versions._

    // shared
    val logbackClassic      = "ch.qos.logback"  %  "logback-classic"   % logbackVersion           % "test"

    // faunadb-java
    val junitInterface      = "com.novocode"    %  "junit-interface"   % junitInterfaceVersion    % "test"
    val harmcrestLibrary    = "org.hamcrest"    %  "hamcrest-library"  % harmcrestLibraryVersion  % "test"
    val junit               = "junit"           %  "junit"             % junitVersion             % "test"
    val snakeYaml           = "org.yaml"        %  "snakeyaml"         % snakeYamlVersion         % "test"

    // faunadb-scala
    val scalaTest           = "org.scalatest"       %% "scalatest"         % scalaTestVersion         % "test"
    val reactiveStreams     = "org.reactivestreams" %  "reactive-streams"  % reactiveStreamsVersion   % "test"
    val monix               = "io.monix"            %% "monix"             % monixVersion             % "test"
  }

  import Compile._
  import Test._

  val jacksonCommon = Seq(jacksonCore, jacksonDatabind, jacksonDatatype)
  val jacksonScala = Seq(jacksonModuleScala, jacksonAnnotations)

  def scalaLang(scalaVersion: String): Seq[ModuleID] = {
    val scalaReflect =
      CrossVersion.partialVersion(scalaVersion) match {
        case Some((2, 11)) => Seq(scalaReflectScala211)
        case Some((2, 12)) => Seq(scalaReflectScala212)
        case _             => Seq.empty
      }

    scalaReflect ++ Seq(scalaJava8Compat)
  }

  // Projects
  val faunadbCommon = jacksonCommon ++ Seq(slf4j, metrics)
  val faunadbJava = Seq(logbackClassic, snakeYaml, junit, junitInterface, harmcrestLibrary)
  def faunadbScala(scalaVersion: String): Seq[ModuleID] = jacksonScala ++ scalaLang(scalaVersion) ++ Seq(logbackClassic, scalaTest, reactiveStreams, monix)

}
