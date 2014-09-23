package net.secloud.core

import org.specs2.mutable._
import java.io.File
import net.secloud.core.crypto._
import net.secloud.core.objects._
import net.secloud.core.utils.StreamUtils._

class RepositoryFileSystemSpec extends Specification {
  "RepositoryFileSystem" should {
    "read files" in TestWorkingDirectory { base ⇒
      val vfs = new NativeFileSystem(base)
      val db = new DirectoryRepositoryDatabase(new File(base, ".secloud"))
      val asymmetricKey = RSA.generate(512, 25)
      val symmetricAlgorithm = AES
      val symmetricAlgorithmKeySize = 16
      val config = Config(asymmetricKey, symmetricAlgorithm, symmetricAlgorithmKeySize, Nil)
      val repo = new Repository(vfs, db, config)

      val commitId1 = repo.init()
      val commit1 = repo.database.readCommit(commitId1, Right(asymmetricKey))
      val rfs1 = new RepositoryFileSystem(db, commit1)

      val commitId2 = repo.commit()
      val commit2 = repo.database.readCommit(commitId2, Right(asymmetricKey))
      val rfs2 = new RepositoryFileSystem(db, commit2)
      rfs2.read(VirtualFile("/a.txt"))(readString) === "Hello World a"
      rfs2.read(VirtualFile("/first/b.txt"))(readString) === "Hello World b"
      rfs2.read(VirtualFile("/first/first-1/c.txt"))(readString) === "Hello World c"

      var lines1 = List.empty[String]
      rfs2.read(VirtualFile("/empty"))(cs ⇒ readLines(cs, l ⇒ lines1 ++= List(l)))
      lines1 === List("")

      var lines2 = List.empty[String]
      rfs2.read(VirtualFile("/bin/script.py"))(cs ⇒ readLines(cs, l ⇒ lines2 ++= List(l)))
      lines2 === List("#!/usr/bin/python", "print 'Hello World!'", "")
    }

    "distinguish between existing and non existing files" in TestWorkingDirectory { base ⇒
      val vfs = new NativeFileSystem(base)
      val db = new DirectoryRepositoryDatabase(new File(base, ".secloud"))
      val asymmetricKey = RSA.generate(512, 25)
      val symmetricAlgorithm = AES
      val symmetricAlgorithmKeySize = 16
      val config = Config(asymmetricKey, symmetricAlgorithm, symmetricAlgorithmKeySize, Nil)
      val repo = new Repository(vfs, db, config)

      val commitId1 = repo.init()
      val commit1 = repo.database.readCommit(commitId1, Right(asymmetricKey))
      val rfs1 = new RepositoryFileSystem(db, commit1)
      rfs1.exists(VirtualFile("/")) === true
      rfs1.exists(VirtualFile("/a.txt")) === false

      val commitId2 = repo.commit()
      val commit2 = repo.database.readCommit(commitId2, Right(asymmetricKey))
      val rfs2 = new RepositoryFileSystem(db, commit2)
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

    "return proper mode" in TestWorkingDirectory { base ⇒
      val vfs = new NativeFileSystem(base)
      val db = new DirectoryRepositoryDatabase(new File(base, ".secloud"))
      val asymmetricKey = RSA.generate(512, 25)
      val symmetricAlgorithm = AES
      val symmetricAlgorithmKeySize = 16
      val config = Config(asymmetricKey, symmetricAlgorithm, symmetricAlgorithmKeySize, Nil)
      val repo = new Repository(vfs, db, config)

      val commitId1 = repo.init()
      val commit1 = repo.database.readCommit(commitId1, Right(asymmetricKey))
      val rfs1 = new RepositoryFileSystem(db, commit1)
      rfs1.mode(VirtualFile("/")) === Directory

      val commitId2 = repo.commit()
      val commit2 = repo.database.readCommit(commitId2, Right(asymmetricKey))
      val rfs2 = new RepositoryFileSystem(db, commit2)
      rfs1.mode(VirtualFile("/")) === Directory
      rfs2.mode(VirtualFile("/first")) === Directory
      rfs2.mode(VirtualFile("/first/first-1")) === Directory
      rfs2.mode(VirtualFile("/a.txt")) === NonExecutableFile
      rfs2.mode(VirtualFile("/first/b.txt")) === NonExecutableFile
      rfs2.mode(VirtualFile("/first/first-1/c.txt")) === NonExecutableFile
      rfs2.mode(VirtualFile("/bin/script.py")) === ExecutableFile
    }

    "yield database objects" in TestWorkingDirectory { base ⇒
      val vfs = new NativeFileSystem(base)
      val db = new DirectoryRepositoryDatabase(new File(base, ".secloud"))
      val asymmetricKey = RSA.generate(512, 25)
      val symmetricAlgorithm = AES
      val symmetricAlgorithmKeySize = 16
      val config = Config(asymmetricKey, symmetricAlgorithm, symmetricAlgorithmKeySize, Nil)
      val repo = new Repository(vfs, db, config)

      val commitId1 = repo.init()
      val commit1 = repo.database.readCommit(commitId1, Right(asymmetricKey))
      val rfs1 = new RepositoryFileSystem(db, commit1)
      rfs1.mode(VirtualFile("/")) === Directory

      val commitId2 = repo.commit()
      val commit2 = repo.database.readCommit(commitId2, Right(asymmetricKey))
      val rfs2 = new RepositoryFileSystem(db, commit2)

      rfs2.obj(VirtualFile("/")) must beAnInstanceOf[Tree]
      rfs2.obj(VirtualFile("/first")) must beAnInstanceOf[Tree]
      rfs2.obj(VirtualFile("/first/first-1")) must beAnInstanceOf[Tree]

      rfs2.obj(VirtualFile("/a.txt")) must beAnInstanceOf[Blob]
      rfs2.obj(VirtualFile("/first/b.txt")) must beAnInstanceOf[Blob]
      rfs2.obj(VirtualFile("/first/first-1/c.txt")) must beAnInstanceOf[Blob]

      rfs2.obj(VirtualFile("/firs")) must throwAn[Exception]
      rfs2.obj(VirtualFile("/firsT")) must throwAn[Exception]
      rfs2.obj(VirtualFile("/a.txta")) must throwAn[Exception]
      rfs2.obj(VirtualFile("/first/first-1/c.txt/foo")) must throwAn[Exception]
    }
  }
}
