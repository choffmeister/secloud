import sbt._
import Keys._
import xerial.sbt.Pack._
import sbtunidoc.Plugin._
import DocPublishPlugin._

object Build extends sbt.Build {
  val commonSettings = Defaults.defaultSettings ++ Seq(
    organization := "net.secloud",
    version := "0.0.1",
    scalaVersion := "2.10.3",
    scalacOptions ++= Seq("-unchecked", "-feature", "-deprecation", "-language:postfixOps", "-encoding", "utf8"),
    scalacOptions <<= baseDirectory.map(bd => Seq("-sourcepath", bd.getAbsolutePath)),
    testOptions in Test += Tests.Argument("junitxml", "console")
  )

  val scctSettings = ScctPlugin.instrumentSettings ++ Seq(
    resourceDirectory in ScctPlugin.ScctTest <<= (resourceDirectory in Test),
    unmanagedResources in ScctPlugin.ScctTest <<= (unmanagedResources in Test)
  )

  val commonProjectSettings = commonSettings ++ scctSettings ++ CoveragePlugin.coverageSettings

  lazy val core = (project in file("core"))
    .settings(commonProjectSettings: _*)

  lazy val commandline = (project in file("commandline"))
    .settings(commonProjectSettings: _*)
    .dependsOn(core)

  lazy val root = (project in file("."))
    .settings(commonSettings: _*)
    .settings(packSettings: _*)
    .settings(unidocSettings: _*)
    .settings(docPublishSettings: _*)
    .settings(
      name := "secloud",
      packMain := Map("secloud" -> "net.secloud.commandline.Application"),
      //docPublishHost := "",
      //docPublishUserName := "",
      //docPublishRemoteDir := ""
    )
    .aggregate(core, commandline)
}
