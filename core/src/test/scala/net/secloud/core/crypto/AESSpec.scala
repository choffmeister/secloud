package net.secloud.core.crypto

import org.junit.runner.RunWith
import org.specs2.mutable._
import org.specs2.runner.JUnitRunner
import net.secloud.core.utils.BinaryReaderWriter._
import java.io._

@RunWith(classOf[JUnitRunner])
class AESSpec extends Specification {
  val plains = List(
    "", "a", "abc", "123456789012345", "1234567890123456",
    "12345678901234567", "123456789012345678901234567891",
    "1234567890123456789012345678912", "12345678901234567890123456789123",
    "\0\0\0abc"
  ).map(_.getBytes("ASCII"))

  "AES" should {
    "en- and decrypt with 128, 192 and 256 bit key size" in {
      for (keySize <- List(16, 24, 32)) {
        for (plain <- plains) {
          val aes = AES.generate(keySize)
          encryptThenDecrypt(aes, aes, plain)
        }
      }

      ok
    }

    "save and load parameters" in {
      for (keySize <- List(16, 24, 32)) {
        for (plain <- plains) {
          val aes1 = AES.generate(keySize)
          val aes2 = AES.load(bytesToStream(streamToBytes(bs => AES.save(bs, aes1))))
          aes1 !== aes2
          encryptThenDecrypt(aes1, aes2, plain)
        }
      }

      ok
    }
  }

  def encryptThenDecrypt(aes1: AES, aes2: AES, plainIn: Array[Byte]): Unit = {
    val encrypted = streamToBytes(bs => aes1.encrypt(bs)(es => es.writeBinary(plainIn)))
    val plainOut = aes2.decrypt(bytesToStream(encrypted))(ds => ds.readBinary())

    plainIn === plainOut
    plainIn !== encrypted
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
