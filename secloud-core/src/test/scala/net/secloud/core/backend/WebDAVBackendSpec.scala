package net.secloud.core.backend

import java.io.File

import net.secloud.core._
import net.secloud.core.crypto._
import net.secloud.core.utils.StreamUtils._
import org.specs2.mutable.Specification

class WebDAVBackendSpec extends Specification {
  "WebDavBackend" should {
    "work" in TestWorkingDirectory { base ⇒
      val vfs = new NativeFileSystem(base)
      val db = new DirectoryRepositoryDatabase(new File(base, ".secloud"))
      val asymmetricKey = RSA.generate(512, 25)
      val symmetricAlgorithm = AES
      val symmetricAlgorithmKeySize = 16
      val config = Config(
        asymmetricKey,
        symmetricAlgorithm,
        symmetricAlgorithmKeySize,
        Nil)
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

      val webdavConfig = WebDAVBackendConfig("", "", "", "")
      val webdav = new WebDAVBackend(webdavConfig)
      webdav.wipe()
      webdav.init()

      val start = System.currentTimeMillis()
      repo.synchronize(webdav)
      val end = System.currentTimeMillis()
      println(s"Took ${(end - start).toDouble / 1000.0} secs")

      ok
    }
  }
}
