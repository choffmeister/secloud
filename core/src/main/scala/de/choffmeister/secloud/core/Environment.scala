package de.choffmeister.secloud.core

import java.io.File
import java.util.Date

case class Environment(
  val currentDirectory: File,
  val userDirectory: File,
  val now: Date
)