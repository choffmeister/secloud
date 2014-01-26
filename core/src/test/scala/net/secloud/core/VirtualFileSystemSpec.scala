package net.secloud.core

import org.specs2.mutable._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import java.util.UUID
import java.io.{InputStream, OutputStream}
import java.io.{File, FileInputStream, FileOutputStream}

@RunWith(classOf[JUnitRunner])
class VirtualFileSystemSpec extends Specification {
  def getTempDir = new File(new File(System.getProperty("java.io.tmpdir")), UUID.randomUUID().toString())

  "NativeFileSystem" should {
    "read and write files" in {
      val base = getTempDir
      build(base)
      val vfs = new NativeFileSystem(base)

      val f1 = VirtualFile(vfs, "/a.txt")
      vfs.read(f1)(s => read(s)) === "Hello World a"

      val f2 = VirtualFile(vfs, "/new.txt")
      f2.exists must beFalse
      vfs.write(f2)(s => write(s, "NEW.TXT"))
      f2.exists must beTrue
      vfs.read(f2)(s => read(s)) === "NEW.TXT"
    }
  }

  def build(base: File) {
    mkdirs(base, Nil)
    mkdirs(base, List("first", "first-1"))
    mkdirs(base, List("first", "first-2"))
    mkdirs(base, List("second", "second-1"))
    mkdirs(base, List("second", "second-2"))
    put(base, List("a.txt"), "Hello World a")
    put(base, List("first", "b.txt"), "Hello World b")
    put(base, List("first", "first-1", "c.txt"), "Hello World c")
    put(base, List("first", "first-2", "d.txt"), "Hello World d")
    put(base, List("second", "second-1", "e.txt"), "Hello World e")
  }

  def mkdirs(base: File, path: List[String]) {
    val file = new File(base, path.mkString(File.separator))
    file.mkdirs()
  }

  def put(base: File, path: List[String], content: String) {
    val file = new File(base, path.mkString(File.separator))
    val stream = new FileOutputStream(file)
    write(stream, content)
    stream.close()
  }

  def write(s: OutputStream, content: String): Unit = {
    val buffer = content.getBytes("ASCII")
    s.write(buffer, 0, buffer.length)
  }

  def read(s: InputStream): String = {
    scala.io.Source.fromInputStream(s).mkString("")
  }
}
