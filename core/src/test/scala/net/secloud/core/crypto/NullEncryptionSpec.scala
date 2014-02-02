package net.secloud.core.crypto

import org.junit.runner.RunWith
import org.specs2.mutable._
import org.specs2.runner.JUnitRunner
import net.secloud.core.utils._
import java.io._

@RunWith(classOf[JUnitRunner])
class NullEncryptionSpec extends Specification {
  "NullEncryption" should {
    "en- and decrypt" in {
      val nu = NullEncryption.generate(0)

      val plainIn = "Hello World".getBytes("UTF-8")
      val encrypted = streamToBytes(bs => nu.encrypt(bs)(es => es.writeBinary(plainIn)))
      val plainOut = nu.decrypt(bytesToStream(encrypted))(ds => ds.readBinary())

      Array(plainIn.length.toByte) ++ plainIn === encrypted
      plainIn === plainOut
    }
  }

  def bytesToStream(bytes: Array[Byte]): InputStream = {
    new ByteArrayInputStream(bytes)
  }

  def streamToBytes(inner: OutputStream => Any): Array[Byte] = {
    val s = new ByteArrayOutputStream()
    inner(s)
    s.toByteArray
  }
}
