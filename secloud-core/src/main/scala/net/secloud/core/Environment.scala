package net.secloud.core

import java.io.File
import java.util.Date

case class Environment(
  currentDirectory: File,
  userDirectory: File)

object Environment {
  def apply(): Environment = Environment(
    currentDirectory = new File(System.getProperty("user.dir")),
    userDirectory = new File(System.getProperty("user.home")))
}
