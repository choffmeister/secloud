package net.secloud.core

import org.specs2.mutable._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import java.util.UUID
import java.io.{InputStream, OutputStream}
import java.io.{File, FileInputStream, FileOutputStream}
import net.secloud.core.utils.StreamUtils._

@RunWith(classOf[JUnitRunner])
class VirtualFileSystemSpec extends Specification {
  def getTempDir = new File(new File(System.getProperty("java.io.tmpdir")), UUID.randomUUID().toString())

  "NativeFileSystem" should {
    "read and write files" in {
      val base = getTempDir
      TestWorkingDirectory.create(base)

      val vfs = new NativeFileSystem(base)

      val f1 = VirtualFile("/a.txt")
      vfs.read(f1)(s => readString(s)) === "Hello World a"

      val f2 = VirtualFile("/new.txt")
      vfs.exists(f2) must beFalse
      vfs.write(f2)(s => writeString(s, "NEW.TXT"))
      vfs.exists(f2) must beTrue
      vfs.read(f2)(s => readString(s)) === "NEW.TXT"
    }
  }
}
