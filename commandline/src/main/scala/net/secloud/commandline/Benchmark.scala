package net.secloud.core

import java.security.MessageDigest
import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.Cipher
import net.secloud.core.security.CryptographicAlgorithms._
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import com.jcraft.jzlib.{GZIPInputStream, GZIPOutputStream}

object Benchmark {
  val megaByteData = (1 to 1024 * 1024).map(_ % 256).map(_.toByte).toArray[Byte]
  val iterations = 100
  val byteCount = iterations * megaByteData.length

  val symmetricAlgorithms = List(NullEncryption, `AES-128`, `AES-192`, `AES-256`)
  val hashAlgorithms = List(`SHA-1`, `SHA-2-256`, `SHA-2-384`, `SHA-2-512`)

  def fullBenchmark() {
    for (sa <- symmetricAlgorithms) {
      if (sa.supported) {
        println(s"${sa.friendlyName} encrypt: ${benchmark(sa, EncryptMode)} MB/s")
      } else {
        println(s"${sa.friendlyName}: not supported")
      }
    }

    for (ha <- hashAlgorithms) {
      println(s"${ha.friendlyName} hash: ${benchmark(ha)} MB/s")
    }

    val gzip = benchmarkGZIP()
    println(s"GZip compress: ${gzip._1} MB/s")
    println(s"GZip decompress: ${gzip._2} MB/s")
  }

  def benchmarkGZIP(): (Double, Double) = {
    val buf = new Array[Byte](8192)

    val ba1 = new ByteArrayOutputStream()
    val gzip1 = new GZIPOutputStream(ba1)
    val runtime1 = benchmark {
      for (i <- 1 to iterations) {
        gzip1.write(megaByteData)
      }
      gzip1.flush()
      gzip1.close()
    }

    val ba2 = new ByteArrayInputStream(ba1.toByteArray)
    val gzip2 = new GZIPInputStream(ba2)
    val runtime2 = benchmark {
      while (gzip2.read(buf, 0, 8192) >= 0) {}
      gzip2.close()
    }

    return (toMegaBytesPerSecond(byteCount, runtime1), toMegaBytesPerSecond(byteCount, runtime2))
  }

  def benchmark(algorithm: SymmetricEncryptionAlgorithm, mode: SymmetricEncryptionMode): Double = {
    val params = algorithm.generateKey()
    val cipher = algorithm.createCipher(mode, params)

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
