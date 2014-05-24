name := "secloud-core"

resolvers ++= Seq(
  "netbeans" at "http://bits.netbeans.org/maven2/"
)

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.0.13",
  "com.jcraft" % "jzlib" % "1.1.3",
  "commons-codec" % "commons-codec" % "1.8",
  "org.bouncycastle" % "bcpkix-jdk15on" % "1.50",
  "org.bouncycastle" % "bcprov-jdk15on" % "1.50",
  "org.netbeans.modules" % "org-netbeans-modules-keyring-impl" % "RELEASE731",
  "org.specs2" %% "specs2" % "2.3.8" % "test"
)
