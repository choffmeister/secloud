name := "secloud-core"

resolvers ++= Seq(
  "netbeans" at "http://bits.netbeans.org/maven2/"
)

libraryDependencies ++= Seq(
  "com.jcraft" % "jzlib" % "1.1.3",
  "commons-codec" % "commons-codec" % "1.8",
  "org.bouncycastle" % "bcprov-jdk15on" % "1.50",
  "org.netbeans.modules" % "org-netbeans-modules-keyring-impl" % "RELEASE731"
)
