package net.secloud.core.crypto

import java.io._
import net.secloud.core.utils.StreamUtils._
import org.apache.commons.codec.binary.Hex
import org.junit.runner.RunWith
import org.specs2.mutable._
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class SHASpec extends Specification {
  val testsSHA1 = Map(
    "" -> "da39a3ee5e6b4b0d3255bfef95601890afd80709",
    "a" -> "86f7e437faa5a7fce15d1ddcb9eaeaea377667b8",
    "hello world" -> "2aae6c35c94fcfb415dbe95f408b9ce91ee846ed"
  )

  val testsSHA256 = Map(
    "" -> "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    "a" -> "ca978112ca1bbdcafac231b39a23dc4da786eff8147c4e72b9807785afee48bb",
    "hello world" -> "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9"
  )

  val testsSHA384 = Map(
    "" -> "38b060a751ac96384cd9327eb1b1e36a21fdb71114be07434c0cc7bf63f6e1da274edebfe76f65fbd51ad2f14898b95b",
    "a" -> "54a59b9f22b0b80880d8427e548b7c23abd873486e1f035dce9cd697e85175033caa88e6d57bc35efae0b5afd3145f31",
    "hello world" -> "fdbd8e75a67f29f701a4e040385e2e23986303ea10239211af907fcbb83578b3e417cb71ce646efd0819dd8c088de1bd"
  )

  val testsSHA512 = Map(
    "" -> "cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e",
    "a" -> "1f40fc92da241694750979ee6cf582f2d5d7d28e18335de05abc54d0560e0f5302860c652bf08d560252aa5e74210546f369fbbbce8c12cfc7957b2652fe9a75",
    "hello world" -> "309ecc489c12d6eb4cc40f50c902f2b4d0ed77ee511a7c7a9bcd3ca86d4cd86f989dd35bc5ff499670da34255b45b0cfd830e81f605dcf7dc5542e93ae9cd76f"
  )

  "SHA1" should {
    "hash" in {
      for (test <- testsSHA1) hashInputStream(SHA1.create(), test._1.getBytes("ASCII")) === Hex.decodeHex(test._2.toCharArray)
      for (test <- testsSHA1) hashOutputStream(SHA1.create(), test._1.getBytes("ASCII")) === Hex.decodeHex(test._2.toCharArray)
      ok
    }
  }

  "SHA256" should {
    "hash" in {
      for (test <- testsSHA256) hashInputStream(SHA256.create(), test._1.getBytes("ASCII")) === Hex.decodeHex(test._2.toCharArray)
      for (test <- testsSHA256) hashOutputStream(SHA256.create(), test._1.getBytes("ASCII")) === Hex.decodeHex(test._2.toCharArray)
      ok
    }
  }

  "SHA384" should {
    "hash" in {
      for (test <- testsSHA384) hashInputStream(SHA384.create(), test._1.getBytes("ASCII")) === Hex.decodeHex(test._2.toCharArray)
      for (test <- testsSHA384) hashOutputStream(SHA384.create(), test._1.getBytes("ASCII")) === Hex.decodeHex(test._2.toCharArray)
      ok
    }
  }

  "SHA512" should {
    "hash" in {
      for (test <- testsSHA512) hashInputStream(SHA512.create(), test._1.getBytes("ASCII")) === Hex.decodeHex(test._2.toCharArray)
      for (test <- testsSHA512) hashOutputStream(SHA512.create(), test._1.getBytes("ASCII")) === Hex.decodeHex(test._2.toCharArray)
      ok
    }
  }

  def hashInputStream(sha: HashAlgorithmInstance, content: Array[Byte]): Array[Byte] = {
    val (hash, result) = bytesAsStream(content) { cs =>
      sha.hash(cs) { hs =>
        var done = false
        while (hs.read() >= 0) {}
      }
    }
    hash
  }

  def hashOutputStream(sha: HashAlgorithmInstance, content: Array[Byte]): Array[Byte] = {
    val cs = new ByteArrayOutputStream()
    val hash = sha.hash(cs) { hs =>
      hs.write(content, 0, content.length)
    }
    hash
  }
}
