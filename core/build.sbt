name := "secloud-core"

resolvers ++= Seq(
  "netbeans" at "http://bits.netbeans.org/maven2/"
)

libraryDependencies ++= Seq(
  "commons-codec" % "commons-codec" % "1.8",
  "org.netbeans.modules" % "org-netbeans-modules-keyring-impl" % "RELEASE731"
)

testOptions in Test += Tests.Argument("junitxml", "console")

ScctPlugin.instrumentSettings

CoveragePlugin.coverageSettings
