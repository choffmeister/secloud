package de.choffmeister.secloud.core

import java.security.MessageDigest
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.Cipher
import de.choffmeister.secloud.core.security.CryptographicAlgorithms._

object Benchmark {
  val megaByteData = (1 to 1024 * 1024).map(_ % 256).map(_.toByte).toArray[Byte]
  val iterations = 100
  val byteCount = iterations * megaByteData.length

  def fullBenchmark() {
    println(s"${`AES-128`.friendlyName} encrypt: ${benchmark(`AES-128`, Cipher.ENCRYPT_MODE)} MB/s")
    println(s"${`AES-192`.friendlyName} encrypt: ${benchmark(`AES-192`, Cipher.ENCRYPT_MODE)} MB/s")
    println(s"${`AES-256`.friendlyName} encrypt: ${benchmark(`AES-256`, Cipher.ENCRYPT_MODE)} MB/s")
    println(s"${`SHA-1`.friendlyName} hash: ${benchmark(`SHA-1`)} MB/s")
    println(s"${`SHA-2-256`.friendlyName} hash: ${benchmark(`SHA-2-256`)} MB/s")
    println(s"${`SHA-2-384`.friendlyName} hash: ${benchmark(`SHA-2-384`)} MB/s")
    println(s"${`SHA-2-512`.friendlyName} hash: ${benchmark(`SHA-2-512`)} MB/s")
  }

  def benchmark(algorithm: SymmetricEncryptionAlgorithm, mode: Int): Double = {
    val key = new SecretKeySpec((1 to algorithm.keySize).map(_.toByte).toArray, algorithm.algorithmName)
    val iv = new IvParameterSpec((1 to algorithm.blockSize).map(_.toByte).toArray)

    val cipher = Cipher.getInstance(algorithm.fullAlgorithmName)
    cipher.init(mode, key, iv)

    val runtime = benchmark {
      for (i <- 1 to iterations) {
        cipher.update(megaByteData)
      }
      cipher.doFinal()
    }

    return toMegaBytesPerSecond(byteCount, runtime)
  }

  def benchmark(algorithm: HashAlgorithm): Double = {
    val digest = MessageDigest.getInstance(algorithm.algorithmName)

    val runtime = benchmark {
      for (i <- 1 to iterations) {
        digest.update(megaByteData)
      }
      digest.digest()
    }

    return toMegaBytesPerSecond(byteCount, runtime)
  }

  def benchmark(inner: => Any): Double = {
    val startTime = System.currentTimeMillis
    inner
    val endTime = System.currentTimeMillis

    return (endTime - startTime).toDouble / 1000.0
  }

  def toMegaBytesPerSecond(byteCount: Long, runtime: Double) = byteCount.toDouble / runtime / 1024.0 / 1024.0
}