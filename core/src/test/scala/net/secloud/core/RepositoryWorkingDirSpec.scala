package net.secloud.core

import org.specs2.mutable._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import java.util.UUID
import java.io.{File, FileOutputStream}
import net.secloud.core.objects._

@RunWith(classOf[JUnitRunner])
class RepositoryWorkingDirSpec extends Specification {
  def getTempDir = new File(new File(System.getProperty("java.io.tmpdir")), UUID.randomUUID().toString())

  "DirectoryRepositoryWorkingDir" should {
    "list directories and files" in {
      val base = getTempDir
      build(base)
      val wd = new DirectoryRepositoryWorkingDir(base)

      wd.list("/") === List(
        WorkingDirElement("/a.txt", List("a.txt"), "a.txt", NonExecutableFileElementMode),
        WorkingDirElement("/first", List("first"), "first", DirectoryElementMode),
        WorkingDirElement("/second", List("second"), "second", DirectoryElementMode)
      )
      wd.list("/first") === List(
        WorkingDirElement("/first/b.txt", List("first", "b.txt"), "b.txt", NonExecutableFileElementMode),
        WorkingDirElement("/first/first-1", List("first", "first-1"), "first-1", DirectoryElementMode),
        WorkingDirElement("/first/first-2", List("first", "first-2"), "first-2", DirectoryElementMode)
      )
      wd.list("/second") === List(
        WorkingDirElement("/second/second-1", List("second", "second-1"), "second-1", DirectoryElementMode),
        WorkingDirElement("/second/second-2", List("second", "second-2"), "second-2", DirectoryElementMode)
      )
      wd.list("/second/second-1") === List(
        WorkingDirElement("/second/second-1/e.txt", List("second", "second-1", "e.txt"), "e.txt", NonExecutableFileElementMode)
      )
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
    val buffer = content.getBytes("UTF-8")
    stream.write(buffer, 0, buffer.length)
    stream.close()
  }
}
