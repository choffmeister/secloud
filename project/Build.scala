import sbt._
import Keys._
import xerial.sbt.Pack._
import com.typesafe.sbteclipse.plugin.EclipsePlugin._

object Build extends sbt.Build {
  val commonSettings = Defaults.defaultSettings ++ Seq(
    organization := "de.choffmeister",
    version := "0.0.0-SNAPSHOT",
    scalaVersion := "2.10.3",
    scalacOptions := Seq("-unchecked", "-feature", "-deprecation", "-language:postfixOps", "-encoding", "utf8"),
    testOptions in Test += Tests.Argument("junitxml", "console"),
    EclipseKeys.withSource := true
  )

  val commonDependencies = Seq(
    "junit" % "junit" % "4.11" % "test",
    "org.specs2" %% "specs2" % "2.2.3" % "test"
  )

  lazy val root = Project(
    id = "secloud",
    base = file("."),
    settings = commonSettings ++ packSettings ++ Seq(
      packMain := Map("secloud" -> "de.choffmeister.secloud.commandline.Application")
    )
  ) aggregate(core, commandline)

  lazy val core = Project(
    id = "core",
    base = file("core"),
    settings = commonSettings ++ Seq(
      libraryDependencies ++= commonDependencies
    )
  )

  lazy val commandline = Project(
    id = "commandline",
    base = file("commandline"),
    settings = commonSettings ++ Seq(
      libraryDependencies ++= commonDependencies
    )
  ) dependsOn(core)
}
