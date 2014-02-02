package net.secloud.core

import org.specs2.mutable._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import java.util.UUID
import java.io.{InputStream, OutputStream}
import java.io.{File, FileInputStream, FileOutputStream}
import net.secloud.core.objects._
import net.secloud.core.crypto._

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

      skipped
    }
  }
}
