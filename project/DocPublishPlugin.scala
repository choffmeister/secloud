import sbt._
import Keys._
import sbtunidoc.Plugin.UnidocKeys._

object DocPublishPlugin extends Plugin {
  val docPublish = taskKey[Unit]("publishes the doc")
  val docPublishHost = settingKey[String]("")
  val docPublishUserName = settingKey[String]("")
  val docPublishRemoteDir = settingKey[String]("")

  lazy val docPublishSettings = Seq[Def.Setting[_]](
    docPublish := {
      val docDir = (unidoc in Compile).value(0)
      val buildVersion = (version in Compile).value
      val host = docPublishHost.value
      val userName = docPublishUserName.value
      val remoteDir = docPublishRemoteDir.value

      publish(docDir, buildVersion, host, userName, remoteDir)
    },

    docPublishHost := "invalid.host.tld",
    docPublishUserName := "username",
    docPublishRemoteDir := "/var/www/api"
  )

  private def publish(docDir: File, buildVersion: String, host: String, userName: String, remoteDir: String) {
    val targetString = s"${userName}@${host}:${remoteDir}/${buildVersion}/"
    val cmd = Seq("rsync", "--delete", "-avz", docDir.getAbsolutePath + "/", targetString)

    println(cmd.mkString(" "))

    val exitValue = cmd.run().exitValue()
    if (exitValue != 0) throw new Exception("Executing rsync failed")
  }
}
