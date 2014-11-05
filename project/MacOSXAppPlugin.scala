package de.choffmeister.sbt

import sbt._
import sbt.Keys._
import xerial.sbt.Pack._

object MacOSXAppPlugin extends Plugin {
  val macosxAppName = SettingKey[String]("macosxAppName")
  val macosxAppTarget = SettingKey[File]("macosxAppTarget")
  val macosxAppMainClass = SettingKey[String]("macosxAppMainClass")
  val macosxAppHighResolution = SettingKey[Boolean]("macosxAppHighResolution")
  val macosxAppJavaProperties = SettingKey[Map[String, String]]("macosxAppJavaProperties")

  val macosxAppJars = TaskKey[Seq[File]]("macosxAppJars")
  val macosxAppPackage = TaskKey[File]("macosxAppPackage")

  lazy val macosxAppSettings = packSettings ++ Seq[Def.Setting[_]](
    macosxAppName := name.value,
    macosxAppTarget := target.value / (macosxAppName.value + ".app"),
    macosxAppHighResolution := true,
    macosxAppJavaProperties := Map.empty,
    macosxAppJars := (pack.value / "lib").listFiles(),
    macosxAppPackage := {
      val appTarget = macosxAppTarget.value
      val contentTarget = appTarget / "Contents"
      val resourceTarget = contentTarget / "Resources"
      val macosTarget = contentTarget / "MacOS"
      val javaTarget = contentTarget / "Java"
      val jars = macosxAppJars.value

      // clean and recreate .app folder
      IO.delete(appTarget)
      appTarget.mkdirs()
      contentTarget.mkdir()
      resourceTarget.mkdir()
      macosTarget.mkdir()
      javaTarget.mkdirs()

      // write Info.plist file
      val info = PList(
        "CFBundleInfoDictionaryVersion" -> PListString("6.0"),
        "CFBundleExecutable" -> PListString("appstub"),
        "CFBundleIconFile" -> PListString("app-icon.icns"),
        "CFBundleName" -> PListString(macosxAppName.value),
        "CFBundlePackageType" -> PListString("APPL"),
        "CFBundleIdentifier" -> PListString(organization.value + "." + name.value),
        "CFBundleVersion" -> PListString(version.value),
        "CFBundleSignature" -> PListString("????"),
        "JVMOptions" -> PListDict(Map(
          "JVMVersion" -> PListString("1.7+"),
          "MainClass" -> PListString(macosxAppMainClass.value),
          "ClassPath" -> PListString(jars.map("$APP_PACKAGE/Contents/Java/" + _.getName).mkString(":")),
          "Properties" -> PListDict(macosxAppJavaProperties.value.map(x => x._1 -> PListString(x._2))),
          "WorkingDirectory" -> PListString("$APP_PACKAGE/Contents/MacOS")
        )),
        "NSHighResolutionCapable" -> PListString(macosxAppHighResolution.value.toString)
      )
      PList.writeToFile(info, contentTarget / "Info.plist")

      // add package info
      IO.write(contentTarget / "PkgInfo", "APPL????")

      // add executable
      IO.copyFile(baseDirectory.value / "../project/JavaAppStub", macosTarget / "appstub")
      (macosTarget / "appstub").setExecutable(true, false)

      // add icon
      IO.copyFile(baseDirectory.value / "src/main/resources/images/app-icon.icns", resourceTarget / "app-icon.icns")

      // add JAR files
      jars.foreach(jar => IO.copyFile(jar, javaTarget / jar.getName))

      appTarget
    }
  )
}
