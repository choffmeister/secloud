package net.secloud.core

import org.specs2.mutable._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import java.util.UUID
import java.io.{InputStream, OutputStream}
import java.io.{File, FileInputStream, FileOutputStream}
import net.secloud.core.objects._
import net.secloud.core.crypto._
import net.secloud.core.utils.StreamUtils._

@RunWith(classOf[JUnitRunner])
class RepositoryFileSystemSpec extends Specification {
  def getTempDir = new File(new File(System.getProperty("java.io.tmpdir")), UUID.randomUUID().toString())

  "RepositoryFileSystem" should {
    "read files" in {
      val base = getTempDir
      TestWorkingDirectory.create(base)

      val vfs = new NativeFileSystem(base)
      val db = new DirectoryRepositoryDatabase(new File(base, ".secloud"))
      val asymmetricKey = RSA.generate(512, 25)
      val symmetricAlgorithm = AES
      val symmetricAlgorithmKeySize = 16
      val config = RepositoryConfig(asymmetricKey, symmetricAlgorithm, symmetricAlgorithmKeySize)
      val repo = new Repository(vfs, db, config)

      val commitId1 = repo.init()
      val rfs1 = new RepositoryFileSystem(db, commitId1, Right(asymmetricKey))

      val treeEntry = repo.snapshot()
      val commitId2 = repo.commit(treeEntry.id, treeEntry.key)
      val rfs2 = new RepositoryFileSystem(db, commitId2, Right(asymmetricKey))
      rfs2.read(VirtualFile("/a.txt"))(readString) === "Hello World a"
      rfs2.read(VirtualFile("/first/b.txt"))(readString) === "Hello World b"
      rfs2.read(VirtualFile("/first/first-1/c.txt"))(readString) === "Hello World c"
    }
  }
}
