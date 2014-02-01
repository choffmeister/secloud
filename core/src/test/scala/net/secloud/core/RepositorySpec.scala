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
class RepositorySpec extends Specification {
  def getTempDir = new File(new File(System.getProperty("java.io.tmpdir")), UUID.randomUUID().toString())

  "Repository" should {
    "work" in {
      val base = getTempDir
      build(base)
      val asymmetricKey = RSA.generate(512, 25)
      val symmetricAlgorithm = AES
      val symmetricAlgorithmKeySize = 16
      val config = RepositoryConfig(asymmetricKey, symmetricAlgorithm, symmetricAlgorithmKeySize)
      val repo = Repository(base, config)
      repo.init()
      repo.commit()

      ok
    }
  }

  def build(base: File) {
    mkdirs(base, Nil)
    mkdirs(base, List("first", "first-1"))
    mkdirs(base, List("first", "first-2"))
    mkdirs(base, List("second", "second-1"))
    mkdirs(base, List("second", "second-2"))
    put(base, List("a.txt"), "Hello World a")
    put(base, List("first", "b.txt"), "Hello World b")
    put(base, List("first", "first-1", "c.txt"), "Hello World c")
    put(base, List("first", "first-2", "d.txt"), "Hello World d")
    put(base, List("second", "second-1", "e.txt"), "Hello World e")
  }

  def mkdirs(base: File, path: List[String]) {
    val file = new File(base, path.mkString(File.separator))
    file.mkdirs()
  }

  def put(base: File, path: List[String], content: String) {
    val file = new File(base, path.mkString(File.separator))
    val stream = new FileOutputStream(file)
    write(stream, content)
    stream.close()
  }

  def write(s: OutputStream, content: String): Unit = {
    val buffer = content.getBytes("ASCII")
    s.write(buffer, 0, buffer.length)
  }

  def read(s: InputStream): String = {
    scala.io.Source.fromInputStream(s).mkString("")
  }
}
