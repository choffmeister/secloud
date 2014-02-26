package net.secloud.core

import org.specs2.mutable._
import java.util.UUID
import java.io.InputStream
import java.io.{File, FileInputStream}
import net.secloud.core.crypto._
import net.secloud.core.utils.StreamUtils._

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

    "distinguish between existing and non existing files" in {
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
      rfs1.exists(VirtualFile("/")) === true
      rfs1.exists(VirtualFile("/a.txt")) === false

      val treeEntry = repo.snapshot()
      val commitId2 = repo.commit(treeEntry.id, treeEntry.key)
      val rfs2 = new RepositoryFileSystem(db, commitId2, Right(asymmetricKey))
      rfs1.exists(VirtualFile("/")) === true
      rfs2.exists(VirtualFile("/a.txt")) === true
      rfs2.exists(VirtualFile("/a2.txt")) === false
      rfs2.exists(VirtualFile("/first/b.txt")) === true
      rfs2.exists(VirtualFile("/first/b2.txt")) === false
      rfs2.exists(VirtualFile("/first/first-1/c.txt")) === true
      rfs2.exists(VirtualFile("/first/first-1/c2.txt")) === false
      rfs2.exists(VirtualFile("/first/first-2/c.txt")) === false
      rfs2.exists(VirtualFile("/first2/first-1/c.txt")) === false
    }

    "return proper mode" in {
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
      rfs1.mode(VirtualFile("/")) === Directory

      val treeEntry = repo.snapshot()
      val commitId2 = repo.commit(treeEntry.id, treeEntry.key)
      val rfs2 = new RepositoryFileSystem(db, commitId2, Right(asymmetricKey))
      rfs1.mode(VirtualFile("/")) === Directory
      rfs2.mode(VirtualFile("/first")) === Directory
      rfs2.mode(VirtualFile("/first/first-1")) === Directory
      rfs2.mode(VirtualFile("/a.txt")) === NonExecutableFile
      rfs2.mode(VirtualFile("/first/b.txt")) === NonExecutableFile
      rfs2.mode(VirtualFile("/first/first-1/c.txt")) === NonExecutableFile
      rfs2.mode(VirtualFile("/bin/script.py")) === ExecutableFile
    }
  }
}
