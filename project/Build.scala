import sbt._
import sbt.Keys._
import xerial.sbt.Pack._
import de.choffmeister.sbt.MacOSXAppPlugin._

object Build extends sbt.Build {
  lazy val dist = TaskKey[File]("dist", "Builds the distribution packages")

  lazy val buildSettings = Seq(
    scalaVersion := "2.11.2",
    scalacOptions ++= Seq("-encoding", "utf8"))

  lazy val coordinateSettings = Seq(
    organization := "net.secloud",
    version := "0.0.1-SNAPSHOT")

  lazy val projectSettings = Defaults.defaultSettings ++ Scalariform.settings ++
      Jacoco.settings ++ buildSettings ++ coordinateSettings

  lazy val core = (project in file("secloud-core"))
    .settings(projectSettings: _*)
    .settings(libraryDependencies ++= Seq(
      "ch.qos.logback" % "logback-classic" % "1.1.2",
      "com.typesafe" % "config" % "1.2.0",
      "com.typesafe.akka" %% "akka-actor" % "2.3.6",
      "com.typesafe.akka" %% "akka-slf4j" % "2.3.6",
      "com.typesafe.akka" %% "akka-testkit" % "2.3.6" % "test",
      "io.spray" %% "spray-can" % "1.3.1",
      "io.spray" %% "spray-testkit" % "1.3.1" % "test",
      "org.specs2" %% "specs2" % "2.4.1" % "test"))
    .settings(name := "secloud-core")

  lazy val server = (project in file("secloud-server"))
    .settings(projectSettings: _*)
    .settings(packSettings: _*)
    .settings(packMain := Map("server" -> "net.secloud.Server"))
    .settings(name := "secloud-server")
    .dependsOn(core)

  lazy val macosx = (project in file("secloud-client-macosx"))
    .settings(projectSettings: _*)
    .settings(packSettings: _*)
    .settings(macosxAppSettings: _*)
    .settings(macosxAppName := "secloud")
    .settings(macosxAppMainClass := "net.secloud.Application")
    .settings(macosxAppIcon := Some(baseDirectory.value / "src/main/resources/images/app-icon.icns"))
    .settings(macosxAppJavaJVMOptions := List(
      "-Dapple.laf.useScreenMenuBar=true",
      "-Dapple.awt.UIElement=true"
    ))
    .settings(macosxAppJavaJars := (pack.value / "lib").listFiles)
    .settings(name := "secloud-client-macosx")
    .dependsOn(core)

  lazy val root = (project in file("."))
    .settings(coordinateSettings: _*)
    .settings(dist <<= (target, pack in server, macosxAppPackage in macosx) map { (target, server, macosx) =>
      val dist = target / "dist"
      IO.delete(dist)

      val serverDist = dist / "secloud-server"
      val serverBin = serverDist / "bin"
      IO.copyDirectory(server, serverDist)
      serverBin.listFiles.foreach(_.setExecutable(true, false))

      val macosxDist = dist / macosx.getName
      val macosxBin = macosxDist / "Contents/MacOS/launcher"
      IO.copyDirectory(macosx, macosxDist)
      macosxBin.setExecutable(true, false)

      dist
    })
    .settings(name := "secloud")
    .aggregate(core, server, macosx)
}

object Jacoco {
  import de.johoop.jacoco4sbt._
  import JacocoPlugin._

  lazy val settings = jacoco.settings ++ reports

  lazy val reports = Seq(
    jacoco.reportFormats in jacoco.Config := Seq(
      XMLReport(encoding = "utf-8"),
      ScalaHTMLReport(withBranchCoverage = true)))
}

object Scalariform {
  import com.typesafe.sbt._
  import com.typesafe.sbt.SbtScalariform._
  import scalariform.formatter.preferences._

  lazy val settings = SbtScalariform.scalariformSettings ++ preferences

  lazy val preferences = Seq(
    ScalariformKeys.preferences := ScalariformKeys.preferences.value
      .setPreference(RewriteArrowSymbols, true)
      .setPreference(SpacesWithinPatternBinders, true)
      .setPreference(CompactControlReadability, false))
}
