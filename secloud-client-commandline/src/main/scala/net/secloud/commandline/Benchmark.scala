package net.secloud.core

import java.io._
import java.util.Random
import net.secloud.core.crypto._
import net.secloud.core.objects._
import com.jcraft.jzlib.{ GZIPInputStream, GZIPOutputStream }

object Benchmark {
  import ObjectSerializer._

  val random = new Random()
  val megaByteDataNonRnd = (1 to 1024 * 1024).map(i ⇒ 0).map(_.toByte).toArray[Byte]
  val megaByteDataRnd = (1 to 1024 * 1024).map(i ⇒ random.nextInt()).map(_.toByte).toArray[Byte]
  val iterations = 64
  val byteCount = iterations * 1024 * 1024

  val tests = List[Any](
    (NullEncryption, 0),
    (AES, 16),
    (AES, 24),
    (AES, 32),
    SHA1,
    SHA256,
    SHA384,
    SHA512,
    "GZip",
    "Blob")

  def fullBenchmark() {
    val format = "%-15s %7.1f MB/s (%s)"

    for (test ← tests) {
      try {
        test match {
          case (algo: SymmetricAlgorithm, ks: Int) ⇒
            val inst = algo.generate(ks)
            val res = benchmarkSymmetricAlgorithm(megaByteDataRnd, inst)
            println(format.format(inst.name, res._1, "encrypt"))
            println(format.format(inst.name, res._2, "decrypt"))
          case algo: HashAlgorithm ⇒
            val inst = algo.create()
            val res = benchmarkHashAlgorithm(megaByteDataRnd, inst)
            println(format.format(inst.name, res, "hash"))
          case "GZip" ⇒
            val res1 = benchmarkGZip(megaByteDataNonRnd)
            println(format.format("GZip", res1._1, "compress, non random data"))
            println(format.format("GZip", res1._2, "decompress, non random data"))
            val res2 = benchmarkGZip(megaByteDataRnd)
            println(format.format("GZip", res2._1, "compress, random data"))
            println(format.format("GZip", res2._2, "decompress, random data"))
          case "Blob" ⇒
            val res1 = benchmarkBlob(megaByteDataNonRnd)
            println(format.format("Blob", res1._1, "write, non random data"))
            println(format.format("Blob", res1._2, "read, non random data"))
            val res2 = benchmarkBlob(megaByteDataRnd)
            println(format.format("Blob", res2._1, "write, random data"))
            println(format.format("Blob", res2._2, "read, random data"))
          case _ ⇒ println("Unknown test ${test}")
        }
      } catch {
        case e: Throwable ⇒
          System.err.println(s"Benchmarking ${test} caused an error")
          System.err.println("Error: " + e.getMessage)
          System.err.println("Type: " + e.getClass.getName)
          System.err.println("Stack trace:")
          e.getStackTrace.map("  " + _).foreach(System.err.println)
          System.exit(1)
      }
    }
  }

  def benchmarkBlob(megaByteData: Array[Byte]): (Double, Double) = {
    val buf = new Array[Byte](8192)

    val rsa = RSA.generate(512, 25)
    val rsaMap = Map(rsa.fingerprint.toSeq -> rsa)
    val aes = AES.generate(32)
    val blob = Blob(ObjectId("000102fdfeff"))

    val ba1 = new ByteArrayOutputStream(byteCount + 100)
    val runtime1 = benchmark {
      signObject(ba1, rsa) { ss ⇒
        writeBlob(ss, blob)
        writeBlobContent(ss, aes) { bs ⇒
          for (i ← 1 to iterations) {
            bs.write(megaByteData)
          }
        }
      }
    }

    val ba2 = new ByteArrayInputStream(ba1.toByteArray)
    val runtime2 = benchmark {
      validateObject(ba2, rsaMap) { ss ⇒
        readBlob(ss)
        readBlobContent(ss, aes) { bs ⇒
          while (bs.read(buf, 0, 8192) >= 0) {}
        }
      }
    }

    return (toMegaBytesPerSecond(byteCount, runtime1), toMegaBytesPerSecond(byteCount, runtime2))
  }

  def benchmarkGZip(megaByteData: Array[Byte]): (Double, Double) = {
    val buf = new Array[Byte](8192)

    val ba1 = new ByteArrayOutputStream(byteCount + 100)
    val gzip1 = new GZIPOutputStream(ba1)
    val runtime1 = benchmark {
      for (i ← 1 to iterations) {
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

  def benchmarkSymmetricAlgorithm(megaByteData: Array[Byte], key: SymmetricAlgorithmInstance): (Double, Double) = {
    val buf = new Array[Byte](8192)

    val ba1 = new ByteArrayOutputStream(byteCount + 100)
    var runtime1 = benchmark {
      key.encrypt(ba1) { cs ⇒
        for (i ← 1 to iterations) cs.write(megaByteData)
      }
    }

    val ba2 = new ByteArrayInputStream(ba1.toByteArray)
    var runtime2 = benchmark {
      key.decrypt(ba2) { cs ⇒
        while (cs.read(buf, 0, 8192) >= 0) {}
      }
    }

    return (toMegaBytesPerSecond(byteCount, runtime1), toMegaBytesPerSecond(byteCount, runtime2))
  }

  def benchmarkHashAlgorithm(megaByteData: Array[Byte], hash: HashAlgorithmInstance): Double = {
    val ba = new ByteArrayOutputStream(byteCount + 100)

    val runtime = benchmark {
      hash.hash(ba) { hs ⇒
        for (i ← 1 to iterations) {
          hs.write(megaByteData)
        }
      }
    }

    return toMegaBytesPerSecond(byteCount, runtime)
  }

  def benchmark(inner: ⇒ Any): Double = {
    val startTime = System.currentTimeMillis
    inner
    val endTime = System.currentTimeMillis

    return (endTime - startTime).toDouble / 1000.0
  }

  def toMegaBytesPerSecond(byteCount: Long, runtime: Double) = byteCount.toDouble / runtime / 1024.0 / 1024.0
}
