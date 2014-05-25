import sbt._
import sbt.Keys._
import sbtunidoc.Plugin._
import com.typesafe.sbt.SbtScalariform
import com.typesafe.sbt.SbtScalariform._

object Build extends sbt.Build {
  lazy val dist = TaskKey[Unit]("dist", "Builds the distribution packages")

  lazy val commonSettings = Defaults.defaultSettings ++ Seq(
    organization := "net.secloud",
    version := "0.0.2",
    scalaVersion := "2.10.4",
    scalacOptions <<= baseDirectory.map(bd =>
      Seq("-encoding", "utf8") ++
      Seq("-sourcepath", bd.getAbsolutePath)),
    testOptions in Test += Tests.Argument("junitxml", "console")
  )

  lazy val format = {
    import scalariform.formatter.preferences._

    FormattingPreferences()
      .setPreference(RewriteArrowSymbols, true)
      .setPreference(SpacesWithinPatternBinders, true)
      .setPreference(CompactControlReadability, false)
  }

  lazy val scalariformSettings = SbtScalariform.scalariformSettings ++ Seq(
    ScalariformKeys.preferences in Compile := format,
    ScalariformKeys.preferences in Test := format
  )

  lazy val scctSettings = ScctPlugin.instrumentSettings ++ Seq(
    resourceDirectory in ScctPlugin.ScctTest <<= (resourceDirectory in Test),
    unmanagedResources in ScctPlugin.ScctTest <<= (unmanagedResources in Test)
  )

  lazy val commonProjectSettings = commonSettings ++ scalariformSettings ++ scctSettings

  lazy val core = (project in file("secloud-core"))
    .settings(commonProjectSettings: _*)

  lazy val commandline = (project in file("secloud-client-commandline"))
    .settings(commonProjectSettings: _*)
    .dependsOn(core)

  lazy val macosx = (project in file("secloud-client-macosx"))
    .settings(commonProjectSettings: _*)
    .dependsOn(core)

  import xerial.sbt.Pack.packArchive
  import de.sciss.sbt.appbundle.AppBundlePlugin.appbundle.appbundle

  lazy val root = (project in file("."))
    .settings(commonSettings: _*)
    .settings(unidocSettings: _*)
    .settings(name := "secloud")
    .settings(
      name := "secloud",
      scalacOptions in (ScalaUnidoc, sbtunidoc.Plugin.UnidocKeys.unidoc) ++=
        Opts.doc.sourceUrl("https://github.com/choffmeister/secloud/blob/master€{FILE_PATH}.scala")
      )
    .settings(
      dist <<= (streams, target, packArchive in commandline, appbundle in macosx) map {
        (s, target, commandline, macosx) =>
      }
    )
    .aggregate(core, commandline)
}
