package net.secloud.core

import org.specs2.mutable._
import java.util.UUID
import java.io.InputStream
import java.io.{File, FileOutputStream}
import net.secloud.core.objects._
import net.secloud.core.crypto._
import net.secloud.core.utils.StreamUtils._

class RepositorySpec extends Specification {
  def getTempDir = new File(new File(System.getProperty("java.io.tmpdir")), UUID.randomUUID().toString())

  "Repository" should {
    "commit" in {
      val base = getTempDir
      TestWorkingDirectory.create(base)

      val asymmetricKey = RSA.generate(512, 25)
      val symmetricAlgorithm = AES
      val symmetricAlgorithmKeySize = 16
      val config = RepositoryConfig(asymmetricKey, symmetricAlgorithm, symmetricAlgorithmKeySize)
      val repo = Repository(base, config)
      repo.init()
      val treeEntry = repo.snapshot()
      repo.commit(treeEntry.id, treeEntry.key)

      ok
    }

    "traverse" in {
      val base = getTempDir
      TestWorkingDirectory.create(base)

      val asymmetricKey = RSA.generate(512, 25)
      val symmetricAlgorithm = AES
      val symmetricAlgorithmKeySize = 16
      val config = RepositoryConfig(asymmetricKey, symmetricAlgorithm, symmetricAlgorithmKeySize)
      val repo = Repository(base, config)
      repo.init()
      val treeEntry = repo.snapshot()
      repo.commit(treeEntry.id, treeEntry.key)

      repo.traverse(VirtualFile("/")) must beAnInstanceOf[Tree]
      repo.traverse(VirtualFile("/first")) must beAnInstanceOf[Tree]
      repo.traverse(VirtualFile("/first/first-1")) must beAnInstanceOf[Tree]

      repo.traverse(VirtualFile("/a.txt")) must beAnInstanceOf[Blob]
      repo.traverse(VirtualFile("/first/b.txt")) must beAnInstanceOf[Blob]
      repo.traverse(VirtualFile("/first/first-1/c.txt")) must beAnInstanceOf[Blob]

      repo.traverse(VirtualFile("/firs")) must throwAn[Exception]
      repo.traverse(VirtualFile("/firsT")) must throwAn[Exception]
      repo.traverse(VirtualFile("/a.txta")) must throwAn[Exception]
      repo.traverse(VirtualFile("/first/first-1/c.txt/foo")) must throwAn[Exception]
    }

    "read" in {
      val base = getTempDir
      TestWorkingDirectory.create(base)

      val asymmetricKey = RSA.generate(512, 25)
      val symmetricAlgorithm = AES
      val symmetricAlgorithmKeySize = 16
      val config = RepositoryConfig(asymmetricKey, symmetricAlgorithm, symmetricAlgorithmKeySize)
      val repo = Repository(base, config)
      repo.init()
      val treeEntry = repo.snapshot()
      repo.commit(treeEntry.id, treeEntry.key)

      repo.read(VirtualFile("/a.txt"))(readString) === "Hello World a"
      repo.read(VirtualFile("/first/b.txt"))(readString) === "Hello World b"
      repo.read(VirtualFile("/first/first-1/c.txt"))(readString) === "Hello World c"
    }
  }
}
