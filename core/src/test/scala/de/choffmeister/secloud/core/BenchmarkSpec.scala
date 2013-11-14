package de.choffmeister.secloud.core

import org.specs2.mutable._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.Cipher
import java.security.MessageDigest

@RunWith(classOf[JUnitRunner])
class BenchmarkSpec extends Specification {
  val megaByteData = (1 to 1024 * 1024).map(_ % 256).map(_.toByte).toArray[Byte]
  val iterations = 50
  val byteCount = iterations * megaByteData.length

  "AES" should {
    "encrypt fast" in {
      val encAes128 = benchmarkSymmetricEncryption("AES", "AES/CBC/NoPadding", Cipher.ENCRYPT_MODE, 16, 16)
      val encAes192 = benchmarkSymmetricEncryption("AES", "AES/CBC/NoPadding", Cipher.ENCRYPT_MODE, 24, 16)
      val encAes256 = benchmarkSymmetricEncryption("AES", "AES/CBC/NoPadding", Cipher.ENCRYPT_MODE, 32, 16)

      println(s"AES-128 Encryption: $encAes128 MB/s")
      println(s"AES-192 Encryption: $encAes192 MB/s")
      println(s"AES-256 Encryption: $encAes256 MB/s")

      encAes128 must beGreaterThan(10.0)
      encAes192 must beGreaterThan(10.0)
      encAes256 must beGreaterThan(10.0)
    }

    "decrypt fast" in {
      val decAes128 = benchmarkSymmetricEncryption("AES", "AES/CBC/NoPadding", Cipher.DECRYPT_MODE, 16, 16)
      val decAes192 = benchmarkSymmetricEncryption("AES", "AES/CBC/NoPadding", Cipher.DECRYPT_MODE, 24, 16)
      val decAes256 = benchmarkSymmetricEncryption("AES", "AES/CBC/NoPadding", Cipher.DECRYPT_MODE, 32, 16)

      println(s"AES-128 Decryption: $decAes128 MB/s")
      println(s"AES-192 Decryption: $decAes192 MB/s")
      println(s"AES-256 Decryption: $decAes256 MB/s")

      decAes128 must beGreaterThan(10.0)
      decAes192 must beGreaterThan(10.0)
      decAes256 must beGreaterThan(10.0)
    }
  }

  "SHA" should {
    "hash fast" in {
      val hashSha160 = benchmarkHashAlgorithm("SHA-1")
      val hashSha256 = benchmarkHashAlgorithm("SHA-256")
      val hashSha384 = benchmarkHashAlgorithm("SHA-384")
      val hashSha512 = benchmarkHashAlgorithm("SHA-512")

      println(s"SHA-1-160 Hashing: $hashSha160 MB/s")
      println(s"SHA-2-256 Hashing: $hashSha256 MB/s")
      println(s"SHA-2-384 Hashing: $hashSha384 MB/s")
      println(s"SHA-2-512 Hashing: $hashSha512 MB/s")

      hashSha160 must beGreaterThan(50.0)
      hashSha256 must beGreaterThan(50.0)
      hashSha384 must beGreaterThan(50.0)
      hashSha512 must beGreaterThan(50.0)
    }
  }

  def benchmarkSymmetricEncryption(algorithmName: String, fullAlgorithmName: String, mode: Int, keySize: Int, ivSize: Int): Double = {
    val key = new SecretKeySpec((1 to keySize).map(_.toByte).toArray, algorithmName)
    val iv = new IvParameterSpec((1 to ivSize).map(_.toByte).toArray)

    val encipher = Cipher.getInstance(fullAlgorithmName)
    encipher.init(mode, key, iv)

    val runtime = benchmark {
      for (i <- 1 to iterations) {
        encipher.update(megaByteData)
      }
      encipher.doFinal()
    }

    return toMegaBytesPerSecond(byteCount, runtime)
  }

  def benchmarkHashAlgorithm(algorithmName: String): Double = {
    val digest = MessageDigest.getInstance(algorithmName)

    val runtime = benchmark {
      for (i <- 1 to iterations) {
        digest.update(megaByteData)
      }
      digest.digest()
    }

    return toMegaBytesPerSecond(byteCount, runtime)
  }

  def toMegaBytesPerSecond(byteCount: Long, runtime: Double) = byteCount.toDouble / runtime / 1024.0 / 1024.0

  def benchmark(inner: => Any): Double = {
    val startTime = System.currentTimeMillis
    inner
    val endTime = System.currentTimeMillis

    return (endTime - startTime).toDouble / 1000.0
  }
}