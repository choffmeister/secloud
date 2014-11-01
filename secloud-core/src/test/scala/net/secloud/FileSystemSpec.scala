package net.secloud

import org.specs2.mutable._

class FileSystemSpec extends Specification {
  import RichFileSystem._
  implicit def toPath(p: String) = Path(p)

  "NativeFileSystem" should {
    "work" in TempDirectory { temp ⇒
      val nfs = new NativeFileSystem(temp)
      nfs.exists("/") === true
      nfs.exists("README.md") === false
      nfs.writeString("README.md", "Hello World!\n")
      nfs.exists("README.md") === true
      nfs.readString("README.md") === "Hello World!\n"
    }
  }
}
