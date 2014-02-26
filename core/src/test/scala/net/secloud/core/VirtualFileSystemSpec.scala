package net.secloud.core

import org.specs2.mutable._
import java.util.UUID
import java.io.InputStream
import java.io.{File, FileOutputStream}
import net.secloud.core.utils.StreamUtils._

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

    "return proper mode" in {
      val base = getTempDir
      TestWorkingDirectory.create(base)

      val vfs = new NativeFileSystem(base)

      vfs.mode(VirtualFile("/")) === Directory
      vfs.mode(VirtualFile("/first")) === Directory
      vfs.mode(VirtualFile("/first/first-1")) === Directory
      vfs.mode(VirtualFile("/a.txt")) === NonExecutableFile
      vfs.mode(VirtualFile("/first/b.txt")) === NonExecutableFile
      vfs.mode(VirtualFile("/first/first-1/c.txt")) === NonExecutableFile
      vfs.mode(VirtualFile("/bin/script.py")) === ExecutableFile
    }
  }
}
