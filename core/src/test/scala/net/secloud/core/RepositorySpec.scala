package net.secloud.core

import org.specs2.mutable._
import java.util.UUID
import java.io.File
import net.secloud.core.objects._
import net.secloud.core.crypto._
import net.secloud.core.utils.StreamUtils._

class RepositorySpec extends Specification {
  def getTempDir = new File(new File(System.getProperty("java.io.tmpdir")), UUID.randomUUID().toString())

  "Repository" should {
    "init" in {
      val base = getTempDir
      TestWorkingDirectory.create(base)

      val asymmetricKey = RSA.generate(512, 25)
      val symmetricAlgorithm = AES
      val symmetricAlgorithmKeySize = 16
      val config = RepositoryConfig(asymmetricKey, symmetricAlgorithm, symmetricAlgorithmKeySize)
      val repo = Repository(base, config)
      repo.init()

      ok
    }

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

    "snapshot recognizes whether something has changed or not" in {
      val base = getTempDir
      TestWorkingDirectory.create(base)

      val asymmetricKey = RSA.generate(512, 25)
      val symmetricAlgorithm = AES
      val symmetricAlgorithmKeySize = 16
      val config = RepositoryConfig(asymmetricKey, symmetricAlgorithm, symmetricAlgorithmKeySize)
      val repo = Repository(base, config)
      repo.init()

      val treeEntry1 = repo.snapshot()
      val commit1 = repo.commit(treeEntry1.id, treeEntry1.key)
      val treeEntry2 = repo.snapshot()
      val commit2 = repo.commit(treeEntry2.id, treeEntry2.key)
      TestWorkingDirectory.put(base, List("first", "new.txt"), "This file is new")
      val treeEntry3 = repo.snapshot()
      val commit3 = repo.commit(treeEntry3.id, treeEntry3.key)

      treeEntry1.id === treeEntry2.id
      repo.fileSystem(commit1).tree(VirtualFile("/")).id === repo.fileSystem(commit2).tree(VirtualFile("/")).id
      treeEntry1.id !== treeEntry3.id
      repo.fileSystem(commit1).tree(VirtualFile("/")).id !== repo.fileSystem(commit3).tree(VirtualFile("/")).id
      repo.fileSystem(commit1).tree(VirtualFile("/first")).id !== repo.fileSystem(commit3).tree(VirtualFile("/first")).id
      repo.fileSystem(commit1).tree(VirtualFile("/second")).id === repo.fileSystem(commit3).tree(VirtualFile("/second")).id
    }
  }
}
