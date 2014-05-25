name := "secloud-macosx"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.0.13",
  "org.specs2" %% "specs2" % "2.3.8" % "test"
)

seq(appbundle.settings: _*)

appbundle.name := "Secloud"

appbundle.mainClass := Some("net.secloud.macosx.Application")

appbundle.icon := Some(file("macosx") / "src" / "main" / "resources" / "images" / "app-icon.icns")

appbundle.workingDirectory := Some(file(appbundle.BundleVar_AppPackage))

appbundle.javaOptions ++= Seq(
  "-Xmx256m"
)

appbundle.systemProperties ++= Seq(
  "apple.awt.UIElement" -> "true"
)
