package net.secloud.core

import java.io.File
import java.util.Date

case class Environment(
  currentDirectory: File,
  userDirectory: File,
  osName: String,
  osArch: String,
  osVersion: String)

object Environment {
  def apply(): Environment = Environment(
    currentDirectory = new File(attr("user.dir")).getAbsoluteFile,
    userDirectory = new File(attr("user.home")).getAbsoluteFile,
    osName = attr("os.name"),
    osArch = attr("os.arch"),
    osVersion = attr("os.version"))

  private def attr(name: String) = System.getProperty(name)
}
