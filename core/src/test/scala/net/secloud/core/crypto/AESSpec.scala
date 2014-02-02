package net.secloud.core.crypto

import org.junit.runner.RunWith
import org.specs2.mutable._
import org.specs2.runner.JUnitRunner
import net.secloud.core.utils._
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
          val aes = AES.generate(keySize).asInstanceOf[AES]
          encryptThenDecrypt(aes, aes, plain)
        }
      }

      ok
    }

    "save and load parameters" in {
      for (keySize <- List(16, 24, 32)) {
        for (plain <- plains) {
          val aes1 = AES.generate(keySize).asInstanceOf[AES]
          val encodedKey = StreamUtils.streamAsBytes(bs => AES.save(bs, aes1))
          val aes2 = StreamUtils.bytesAsStream(encodedKey)(ks => AES.load(ks)).asInstanceOf[AES]
          aes1 !== aes2
          encryptThenDecrypt(aes1, aes2, plain)
        }
      }

      ok
    }
  }

  def encryptThenDecrypt(aes1: AES, aes2: AES, plainIn: Array[Byte]): Unit = {
    val encrypted = StreamUtils.streamAsBytes(bs => aes1.encrypt(bs)(es => es.writeBinary(plainIn)))
    val plainOut = StreamUtils.bytesAsStream(encrypted)(bs => aes2.decrypt(bs)(ds => ds.readBinary()))

    plainIn === plainOut
    plainIn !== encrypted
  }
}
