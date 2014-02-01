package net.secloud.core.crypto

import java.io._
import org.junit.runner.RunWith
import org.specs2.mutable._
import org.specs2.runner.JUnitRunner
import net.secloud.core.utils.BinaryReaderWriter._
import org.apache.commons.codec.binary.Hex

@RunWith(classOf[JUnitRunner])
class RSASpec extends Specification {
  val plains = List(
    "", "a", "abc", "123456789012345", "1234567890123456",
    "12345678901234567", "123456789012345678901234567891",
    "1234567890123456789012345678912", "12345678901234567890123456789123",
    "\0\0\0abc"
  ).map(_.getBytes("ASCII"))

  val keys = List(
    Seq[Byte](0),
    Seq[Byte](1,2),
    Seq[Byte](3,4,5),
    (1 to 1024).map(_.toByte),
    Seq[Byte](0, 0, 0, 0, 1, 2, 3, 4, -4, -3, -2, -1)
  ).map(_.toArray)

  "RSA" should {
    "en- and decrypt with 512, 1024 and 2048 bit RSA key size" in {
      for (strength <- List(512, 1024, 2048)) {
        val rsa = RSA.generate(strength, 25).asInstanceOf[RSA]
        for (plain <- plains) {
          encryptThenDecrypt(rsa, rsa, plain)
        }
      }

      ok
    }

    "wrap and unwrap secret keys with 512, 1024 and 2048 bit RSA key size" in {
      for (strength <- List(512, 1024, 2048)) {
        val rsa = RSA.generate(strength, 25)
        for (key <- keys) {
          val wrappedKey = rsa.wrapKey(key)
          val unwrappedKey = rsa.unwrapKey(wrappedKey)

          key === unwrappedKey
        }
      }

      ok
    }

    "sign and validate with 512, 1024 and 2048 bit RSA key size" in {
      for (strength <- List(512, 1024, 2048)) {
        def changeFirst(arr: Array[Byte]) = Array((arr.head ^ 1).toByte) ++ arr.tail
        def changeLast(arr: Array[Byte]) = arr.take(arr.length - 1) ++ Array((arr.last ^ 1).toByte)

        val rsa = RSA.generate(strength, 25)

        for (hash <- List("00", "0000", "ff", "ffff", "295a8b3afafedca56bbc11bca7e1c1ac8521cf93").map(hex => Hex.decodeHex(hex.toCharArray))) {
          val sig = rsa.signHash(hash)

          rsa.validateHash(Array[Byte](0) ++ hash, sig) === false
          rsa.validateHash(hash ++ Array[Byte](0), sig) === false
          rsa.validateHash(hash, Array[Byte](0) ++ sig) === false
          rsa.validateHash(hash, sig ++ Array[Byte](0)) === false
          rsa.validateHash(changeFirst(hash), sig) === false
          rsa.validateHash(changeLast(hash), sig) === false
          rsa.validateHash(hash, changeFirst(sig)) === false
          rsa.validateHash(hash, changeLast(sig)) === false
          rsa.validateHash(hash, sig) === true
        }
      }

      ok
    }

    "load PEM encoded RSA keys (unencrypted)" in {
      for (plain <- plains) {
        encryptThenDecryptFromPEM("plain", None, plain)
      }

      ok
    }

    "load PEM encoded RSA keys (DES, DES3, AES-128, AES-192 and AES-256)" in {
      skipped("Still decrypted PEM files with JCE")
      for (algorithm <- List("des", "des3", "aes128", "aes192", "aes256")) {
        for (plain <- plains) {
          encryptThenDecryptFromPEM(algorithm, Some("pass".toCharArray), plain)
        }
      }

      ok
    }
  }

  def encryptThenDecryptFromPEM(name: String, password: Option[Array[Char]], plainIn: Array[Byte]): Unit = {
    val rsa1 = RSA.loadFromPEM(resource(s"/rsa/rsa-${name}.pub"), None)
    rsa1.isPrivate === false
    val rsa2 = RSA.loadFromPEM(resource(s"/rsa/rsa-${name}.key"), password)
    rsa2.isPrivate === true

    encryptThenDecrypt(rsa1, rsa2, plainIn)
  }

  def encryptThenDecrypt(rsa1: RSA, rsa2: RSA, plainIn: Array[Byte]): Unit = {
    val encrypted = rsa1.encrypt(plainIn)
    val plainOut = rsa2.decrypt(encrypted)

    plainIn === plainOut
    plainIn !== encrypted
  }

  def resource(path: String): InputStream = this.getClass.getResourceAsStream(path)
}
