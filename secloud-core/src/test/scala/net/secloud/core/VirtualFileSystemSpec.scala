package net.secloud.core

import org.specs2.mutable._
import java.io.File
import net.secloud.core.utils.StreamUtils._

class VirtualFileSystemSpec extends Specification {
  "VirtualFile" should {
    "recognize children and parents" in {
      val f1 = VirtualFile("/")
      val f2 = VirtualFile("/README.md")
      val f3 = VirtualFile("/src")
      val f4 = VirtualFile("/src/foo.txt")

      f1.isParentOf(f1) === false
      f1.isChildOf(f1) === false
      f2.isParentOf(f2) === false
      f2.isChildOf(f2) === false

      f1.isParentOf(f2) === true
      f1.isParentOf(f3) === true
      f1.isParentOf(f4) === true

      f2.isChildOf(f1) === true
      f3.isChildOf(f1) === true
      f4.isChildOf(f1) === true

      f3.isChildOf(f2) === false
      f3.isParentOf(f2) === false
      f2.isChildOf(f3) === false
      f2.isParentOf(f3) === false

      f3.isParentOf(f4) === true
      f4.isChildOf(f3) === true
    }
  }
  "NativeFileSystem" should {
    "read and write files" in TestWorkingDirectory { base ⇒
      val vfs = new NativeFileSystem(base)

      val f1 = VirtualFile("/a.txt")
      vfs.read(f1)(s ⇒ readString(s)) === "Hello World a"

      val f2 = VirtualFile("/new.txt")
      vfs.exists(f2) must beFalse
      vfs.write(f2)(s ⇒ writeString(s, "NEW.TXT"))
      vfs.exists(f2) must beTrue
      vfs.read(f2)(s ⇒ readString(s)) === "NEW.TXT"
    }

    "return proper mode" in TestWorkingDirectory { base ⇒
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
