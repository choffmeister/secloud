name := "secloud-commandline"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.0.13",
  "org.rogach" %% "scallop" % "0.9.4",
  "org.specs2" %% "specs2" % "2.3.8" % "test"
)

scalacOptions in (Compile, doc) ++=
  Opts.doc.sourceUrl("https://github.com/choffmeister/secloud/blob/master/commandline€{FILE_PATH}.scala")
