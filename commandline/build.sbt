name := "secloud-commandline"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.0.13",
  "org.rogach" %% "scallop" % "0.9.4",
  "org.specs2" %% "specs2" % "2.3.8" % "test"
)

packSettings

packMain := Map("secloud" -> "net.secloud.commandline.Application")

packExtraClasspath := Map("secloud" -> Seq("${PROG_HOME}/conf"))

pack <<= (baseDirectory, pack, streams) map { (baseDirectory: File, value: File, s) =>
  val confSourceDir = baseDirectory / "src/main/resources"
  val confTargetDir = baseDirectory / "target/pack/conf"
  confTargetDir.mkdirs()
  IO.copyFile(confSourceDir / "logback.xml.dist", confTargetDir / "logback.xml")
  s.log.info("Done copying config files.")
  value
}
