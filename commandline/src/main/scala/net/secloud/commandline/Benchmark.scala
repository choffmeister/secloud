package net.secloud.core

import java.security.MessageDigest
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.Cipher
import net.secloud.core.security.CryptographicAlgorithms._

object Benchmark {
  val megaByteData = (1 to 1024 * 1024).map(_ % 256).map(_.toByte).toArray[Byte]
  val iterations = 100
  val byteCount = iterations * megaByteData.length

  val symmetricAlgorithms = List(`AES-128`, `AES-192`, `AES-256`)
  val hashAlgorithms = List(`SHA-1`, `SHA-2-256`, `SHA-2-384`, `SHA-2-512`)

  def fullBenchmark() {
    for (sa <- symmetricAlgorithms) {
      if (sa.supported) {
        println(s"${sa.friendlyName} encrypt: ${benchmark(sa, Cipher.ENCRYPT_MODE)} MB/s")
      } else {
        println(s"${sa.friendlyName}: not supported")
      }
    }

    for (ha <- hashAlgorithms) {
      println(s"${ha.friendlyName} hash: ${benchmark(ha)} MB/s")
    }
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
