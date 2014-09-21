package net.secloud.core

import org.specs2.mutable._
import org.apache.commons.codec.binary.Hex
import java.io.File
import net.secloud.core.objects._
import net.secloud.core.crypto._
import net.secloud.core.utils.StreamUtils._

class RepositorySpec extends Specification {
  "Repository" should {
    "init" in TestWorkingDirectory { base ⇒
      val asymmetricKey = RSA.generate(512, 25)
      val symmetricAlgorithm = AES
      val symmetricAlgorithmKeySize = 16
      val config = Config(asymmetricKey, symmetricAlgorithm, symmetricAlgorithmKeySize)
      val repo = Repository(base, config)
      repo.init()

      ok
    }

    "commit" in TestWorkingDirectory { base ⇒
      val asymmetricKey = RSA.generate(512, 25)
      val symmetricAlgorithm = AES
      val symmetricAlgorithmKeySize = 16
      val config = Config(asymmetricKey, symmetricAlgorithm, symmetricAlgorithmKeySize)
      val repo = Repository(base, config)
      repo.init()
      repo.commit()

      val fileContentHash = repo.fileSystem(repo.headCommit).hash(VirtualFile("/first/b.txt"))
      Hex.encodeHexString(fileContentHash.get.toArray) === "7670b2ccc9740c2741f029f9212c14da0a855157"
    }

    "commit with hints" in TestWorkingDirectory { base ⇒
      val asymmetricKey = RSA.generate(512, 25)
      val symmetricAlgorithm = AES
      val symmetricAlgorithmKeySize = 16
      val config = Config(asymmetricKey, symmetricAlgorithm, symmetricAlgorithmKeySize)
      val repo = Repository(base, config)
      repo.init()
      repo.commit()
      repo.commitWithChangeHints(List(VirtualFile("/")))

      ok
    }

    "commit does not commit two identical snapshots in a row" in TestWorkingDirectory { base ⇒
      val asymmetricKey = RSA.generate(512, 25)
      val symmetricAlgorithm = AES
      val symmetricAlgorithmKeySize = 16
      val config = Config(asymmetricKey, symmetricAlgorithm, symmetricAlgorithmKeySize)
      val repo = Repository(base, config)
      repo.init()

      val commitId1 = repo.commit()
      val commitId2 = repo.commit()
      new File(base, "a.txt").delete()
      val commitId3 = repo.commit()

      commitId1 === commitId2
      commitId1 !== commitId3
    }

    "snapshot recognizes whether something has changed or not" in TestWorkingDirectory { base ⇒
      val asymmetricKey = RSA.generate(512, 25)
      val symmetricAlgorithm = AES
      val symmetricAlgorithmKeySize = 16
      val config = Config(asymmetricKey, symmetricAlgorithm, symmetricAlgorithmKeySize)
      val repo = Repository(base, config)
      repo.init()

      val commitId1 = repo.commit()
      val commit1 = repo.database.readCommit(commitId1, Right(asymmetricKey))
      val commitId2 = repo.commit()
      val commit2 = repo.database.readCommit(commitId2, Right(asymmetricKey))
      new File(new File(base, "first"), "b.txt").delete()
      val commitId3 = repo.commit()
      val commit3 = repo.database.readCommit(commitId3, Right(asymmetricKey))

      commit1.tree.id === commit2.tree.id
      repo.fileSystem(commit1).tree(VirtualFile("/")).id === repo.fileSystem(commit2).tree(VirtualFile("/")).id
      commit2.tree.id !== commit3.tree.id
      repo.fileSystem(commit1).tree(VirtualFile("/")).id !== repo.fileSystem(commit3).tree(VirtualFile("/")).id
      repo.fileSystem(commit1).tree(VirtualFile("/first")).id !== repo.fileSystem(commit3).tree(VirtualFile("/first")).id
      repo.fileSystem(commit1).tree(VirtualFile("/second")).id === repo.fileSystem(commit3).tree(VirtualFile("/second")).id
    }
  }
}
