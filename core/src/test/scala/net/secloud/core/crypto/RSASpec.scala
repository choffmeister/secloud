package net.secloud.core.crypto

import java.io._
import org.apache.commons.codec.binary.Hex
import org.specs2.mutable._

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

    "(de)serialize public keys" in {
      val rsa1 = RSA.generate(512, 25)
      val bs1 = new ByteArrayOutputStream()
      RSA.save(bs1, rsa1, false)
      val bs2 = new ByteArrayInputStream(bs1.toByteArray)
      val rsa2 = RSA.load(bs2)

      rsa1.isPrivate === true
      rsa2.isPrivate === false

      val plain = Array[Byte](0,1,2,3,4)
      val encrypted = rsa2.encrypt(plain)
      val decrypted = rsa1.decrypt(encrypted)

      plain === decrypted
      plain !== encrypted
    }

    "(de)serialize private keys" in {
      val rsa1 = RSA.generate(512, 25)
      val bs1 = new ByteArrayOutputStream()
      RSA.save(bs1, rsa1, true)
      val bs2 = new ByteArrayInputStream(bs1.toByteArray)
      val rsa2 = RSA.load(bs2)

      rsa1.isPrivate === true
      rsa2.isPrivate === true

      val plain = Array[Byte](0,1,2,3,4)
      val encrypted = rsa1.encrypt(plain)
      val decrypted = rsa2.decrypt(encrypted)

      plain === decrypted
      plain !== encrypted
    }

    "create proper fingerprints" in {
      val rsa1a = RSA.generate(512, 25)
      val bs1 = new ByteArrayOutputStream()
      RSA.save(bs1, rsa1a, false)
      val bs2 = new ByteArrayInputStream(bs1.toByteArray)
      val rsa1b = RSA.load(bs2)
      val rsa2 = RSA.generate(512)

      rsa1a.fingerprint === rsa1b.fingerprint
      rsa1a.fingerprint !== rsa2.fingerprint
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

    "save PEM encoded RSA keys (unencrypted)" in {
      val rsa = RSA.generate(512, 25).asInstanceOf[RSA]

      val bs1a = new ByteArrayOutputStream()
      RSA.saveToPEM(bs1a, rsa, true)
      val bs1b = new ByteArrayInputStream(bs1a.toByteArray)
      val rsaPriv = RSA.loadFromPEM(bs1b)
      rsaPriv.isPrivate === true

      val bs2a = new ByteArrayOutputStream()
      RSA.saveToPEM(bs2a, rsa, false)
      val bs2b = new ByteArrayInputStream(bs2a.toByteArray)
      val rsaPub = RSA.loadFromPEM(bs2b)
      rsaPub.isPrivate === false

      val plain = Array[Byte](0,1,2,3)
      val encrypted = rsaPub.encrypt(plain)
      val decrypted = rsaPriv.decrypt(encrypted)

      plain === decrypted
      plain !== encrypted
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
