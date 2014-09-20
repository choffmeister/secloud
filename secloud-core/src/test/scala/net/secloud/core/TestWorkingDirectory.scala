package net.secloud.core

import java.io.{ File, FileOutputStream }
import java.util.UUID

import net.secloud.core.utils.StreamUtils._
import org.specs2.execute._

object TempDirectory {
  def apply[R: AsResult](a: File ⇒ R) = {
    val temp = createTemporaryDirectory("")
    try {
      AsResult.effectively(a(temp))
    } finally {
      removeTemporaryDirectory(temp)
    }
  }

  def createTemporaryDirectory(suffix: String): File = {
    val base = new File(new File(System.getProperty("java.io.tmpdir")), "gittimeshift")
    val dir = new File(base, UUID.randomUUID().toString + suffix)
    dir.mkdirs()
    dir
  }

  def removeTemporaryDirectory(dir: File): Unit = {
    def recursion(f: File): Unit = {
      if (f.isDirectory) {
        f.listFiles().foreach(child ⇒ recursion(child))
      }
      f.delete()
    }
    recursion(dir)
  }
}

object TestWorkingDirectory {
  def apply[R: AsResult](a: File ⇒ R) = {
    val temp = TempDirectory.createTemporaryDirectory("")
    try {
      fill(temp)
      AsResult.effectively(a(temp))
    } finally {
      TempDirectory.removeTemporaryDirectory(temp)
    }
  }

  private def fill(base: File) {
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

  private def mkdirs(base: File, path: List[String]) {
    val file = TestWorkingDirectory.toFile(base, path)
    file.mkdirs()
  }

  private def put(base: File, path: List[String], content: String) {
    val file = TestWorkingDirectory.toFile(base, path)
    val stream = new FileOutputStream(file)
    writeString(stream, content)
    stream.close()
  }

  private def makeExecutable(base: File, path: List[String]) {
    val file = TestWorkingDirectory.toFile(base, path)
    file.setExecutable(true, false)
  }

  private def toFile(base: File, path: List[String]): File = new File(base, path.mkString(File.separator))
}
