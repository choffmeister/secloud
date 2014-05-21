import sbt.Keys._
import sbt._
import sbtunidoc.Plugin._
import xerial.sbt.Pack._
import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform._

object Build extends sbt.Build {
  lazy val commonSettings = Defaults.defaultSettings ++ Seq(
    organization := "net.secloud",
    version := "0.0.1",
    scalaVersion := "2.10.4",
    scalacOptions ++= Seq("-encoding", "utf8"),
    scalacOptions <<= baseDirectory.map(bd => Seq("-sourcepath", bd.getAbsolutePath)),
    testOptions in Test += Tests.Argument("junitxml", "console")
  )

  val format = {
    import scalariform.formatter.preferences._

    FormattingPreferences()
      .setPreference(RewriteArrowSymbols, true)
      .setPreference(SpacesWithinPatternBinders, true)
      .setPreference(CompactControlReadability, false)
  }

  val scalariformSettings = SbtScalariform.scalariformSettings ++ Seq(
    ScalariformKeys.preferences in Compile := format,
    ScalariformKeys.preferences in Test := format
  )

  val scctSettings = ScctPlugin.instrumentSettings ++ Seq(
    resourceDirectory in ScctPlugin.ScctTest <<= (resourceDirectory in Test),
    unmanagedResources in ScctPlugin.ScctTest <<= (unmanagedResources in Test)
  )

  val commonProjectSettings = commonSettings ++ scalariformSettings ++ scctSettings

  lazy val core = (project in file("core"))
    .settings(commonProjectSettings: _*)

  lazy val commandline = (project in file("commandline"))
    .settings(commonProjectSettings: _*)
    .dependsOn(core)

  lazy val root = (project in file("."))
    .settings(commonSettings: _*)
    .settings(packSettings: _*)
    .settings(unidocSettings: _*)
    .settings(
      name := "secloud",
      packMain := Map("secloud" -> "net.secloud.commandline.Application")
    )
    .aggregate(core, commandline)
}
