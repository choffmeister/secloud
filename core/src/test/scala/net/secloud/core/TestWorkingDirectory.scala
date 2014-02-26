package net.secloud.core

import java.io._
import net.secloud.core.utils.StreamUtils._

object TestWorkingDirectory {
  def create(base: File) {
    mkdirs(base, Nil)
    mkdirs(base, List("first", "first-1"))
    mkdirs(base, List("first", "first-2"))
    mkdirs(base, List("second", "second-1"))
    mkdirs(base, List("second", "second-2"))
    mkdirs(base, List("bin"))
    put(base, List("a.txt"), "Hello World a")
    put(base, List("first", "b.txt"), "Hello World b")
    put(base, List("first", "first-1", "c.txt"), "Hello World c")
    put(base, List("first", "first-2", "d.txt"), "Hello World d")
    put(base, List("second", "second-1", "e.txt"), "Hello World e")
    put(base, List("bin", "script.py"), "#!/usr/bin/python\n\nprint 'Hello World!'")
    makeExecutable(base, List("bin", "script.py"))
  }

  def mkdirs(base: File, path: List[String]) {
    val file = TestWorkingDirectory.toFile(base, path)
    file.mkdirs()
  }

  def put(base: File, path: List[String], content: String) {
    val file = TestWorkingDirectory.toFile(base, path)
    val stream = new FileOutputStream(file)
    writeString(stream, content)
    stream.close()
  }

  def makeExecutable(base: File, path: List[String]) {
    val file = TestWorkingDirectory.toFile(base, path)
    file.setExecutable(true, false)
  }

  def toFile(base: File, path: List[String]): File = new File(base, path.mkString(File.separator))
}
