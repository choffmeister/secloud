package net.secloud.core

import net.secloud.core.security._
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import com.jcraft.jzlib.{GZIPInputStream, GZIPOutputStream}

object Benchmark {
  val megaByteData = (1 to 1024 * 1024).map(_ % 256).map(_.toByte).toArray[Byte]
  val iterations = 100
  val byteCount = iterations * megaByteData.length

  val symmetricAlgorithms = List(NullEncryption, `AES-128`, `AES-192`, `AES-256`)
  val hashAlgorithms = List(`SHA-1`, `SHA-2-256`, `SHA-2-384`, `SHA-2-512`)

  def fullBenchmark() {
    val format = "%-15s %7.1f MB/s (%s)"
    val format2 = "%-15s              (unsupported)"

    for (sa <- symmetricAlgorithms) {
      if (sa.supported) {
        val res = benchmark(sa)
        println(format.format(sa.friendlyName, res._1, "encrypt"))
        println(format.format(sa.friendlyName, res._2, "decrypt"))
      } else {
        println(format2.format(sa.friendlyName))
      }
    }

    for (ha <- hashAlgorithms) {
      val res = benchmark(ha)
      println(format.format(ha.friendlyName, res, "hash"))
    }

    {
      val res = benchmarkGZIP()
      println(format.format("GZip", res._1, "compress"))
      println(format.format("GZip", res._2, "decompress"))
    }
  }

  def benchmarkGZIP(): (Double, Double) = {
    val buf = new Array[Byte](8192)

    val ba1 = new ByteArrayOutputStream(byteCount + 100)
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

  def benchmark(algorithm: SymmetricAlgorithm): (Double, Double) = {
    val buf = new Array[Byte](8192)
    val params = algorithm.generateParameters()

    val ba1 = new ByteArrayOutputStream(byteCount + 100)
    val cs1 = algorithm.wrapStream(ba1, params)
    val runtime1 = benchmark {
      for (i <- 1 to iterations) {
        cs1.write(megaByteData)
      }
      cs1.flush()
      cs1.close()
    }

    val ba2 = new ByteArrayInputStream(ba1.toByteArray)
    val cs2 = algorithm.wrapStream(ba2, params)
    val runtime2 = benchmark {
      while (cs2.read(buf, 0, 8192) >= 0) {}
      cs2.close()
    }

    return (toMegaBytesPerSecond(byteCount, runtime1), toMegaBytesPerSecond(byteCount, runtime2))
  }

  def benchmark(algorithm: HashAlgorithm): Double = {
    val ba = new ByteArrayOutputStream(byteCount + 100)
    val hs = algorithm.wrapStream(ba)
    val runtime = benchmark {
      for (i <- 1 to iterations) {
        hs.write(megaByteData)
      }
      hs.flush()
      hs.close()
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
