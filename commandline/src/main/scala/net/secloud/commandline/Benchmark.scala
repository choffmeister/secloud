package net.secloud.core

import net.secloud.core.crypto._
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import com.jcraft.jzlib.{GZIPInputStream, GZIPOutputStream}

object Benchmark {
  val megaByteData = (1 to 1024 * 1024).map(_ % 256).map(_.toByte).toArray[Byte]
  val iterations = 100
  val byteCount = iterations * megaByteData.length

  val tests = List(
    (NullEncryption, 0),
    (AES, 16),
    (AES, 24),
    (AES, 32),
    SHA1,
    SHA256,
    SHA384,
    SHA512,
    "GZIP"
  )

  def fullBenchmark() {
    val format = "%-15s %7.1f MB/s (%s)"

    for (test <- tests) {
      try {
        test match {
          case (algo: SymmetricAlgorithm, ks: Int) =>
            val inst = algo.generate(ks)
            val res = benchmarkSymmetricAlgorithm(inst)
            println(format.format(inst.name, res._1, "encrypt"))
            println(format.format(inst.name, res._2, "decrypt"))
          case algo: HashAlgorithm =>
            val inst = algo.create()
            val res = benchmarkHashAlgorithm(inst)
            println(format.format(inst.name, res, "hash"))
          case "GZIP" =>
            val res = benchmarkGZIP()
            println(format.format("GZip", res._1, "compress"))
            println(format.format("GZip", res._2, "decompress"))
          case _ => println("Unknown test ${test}")
        }
      } catch {
        case e: Throwable => println(s"Benchmarking ${test} caused an error")
      }
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

  def benchmarkSymmetricAlgorithm(key: SymmetricAlgorithmInstance): (Double, Double) = {
    val buf = new Array[Byte](8192)

    val ba1 = new ByteArrayOutputStream(byteCount + 100)
    var runtime1 = benchmark {
      key.encrypt(ba1) { cs =>
        for (i <- 1 to iterations) cs.write(megaByteData)
      }
    }

    val ba2 = new ByteArrayInputStream(ba1.toByteArray)
    var runtime2 = benchmark {
      key.decrypt(ba2) { cs =>
        while (cs.read(buf, 0, 8192) >= 0) {}
      }
    }

    return (toMegaBytesPerSecond(byteCount, runtime1), toMegaBytesPerSecond(byteCount, runtime2))
  }

  def benchmarkHashAlgorithm(hash: HashAlgorithmInstance): Double = {
    val ba = new ByteArrayOutputStream(byteCount + 100)

    val runtime = benchmark {
      hash.hash(ba) { hs =>
        for (i <- 1 to iterations) {
          hs.write(megaByteData)
        }
      }
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
