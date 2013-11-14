package de.choffmeister.secloud.core

import org.specs2.mutable._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner

import javax.crypto.spec.SecretKeySpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.Cipher

@RunWith(classOf[JUnitRunner])
class BenchmarkSpec extends Specification {
  val megaByteData = (1 to 1024 * 1024).map(_ % 256).map(_.toByte).toArray[Byte]
  val iterations = 25
  
  "AES" should {
    "encrypt fast" in {
      val encAes128 = benchmark("AES", "AES/CBC/NoPadding", Cipher.ENCRYPT_MODE, 16, 16)
      val encAes192 = benchmark("AES", "AES/CBC/NoPadding", Cipher.ENCRYPT_MODE, 24, 16)
      val encAes256 = benchmark("AES", "AES/CBC/NoPadding", Cipher.ENCRYPT_MODE, 32, 16)
      
      println(s"AES-128 Encryption: $encAes128 MB/s")
      println(s"AES-192 Encryption: $encAes192 MB/s")
      println(s"AES-256 Encryption: $encAes256 MB/s")
      
      encAes128 must beGreaterThan(10.0)
      encAes192 must beGreaterThan(10.0)
      encAes256 must beGreaterThan(10.0)
    }
    
    "decrypt fast" in {
      val decAes128 = benchmark("AES", "AES/CBC/NoPadding", Cipher.DECRYPT_MODE, 16, 16)
      val decAes192 = benchmark("AES", "AES/CBC/NoPadding", Cipher.DECRYPT_MODE, 24, 16)
      val decAes256 = benchmark("AES", "AES/CBC/NoPadding", Cipher.DECRYPT_MODE, 32, 16)
      
      println(s"AES-128 Decryption: $decAes128 MB/s")
      println(s"AES-192 Decryption: $decAes192 MB/s")
      println(s"AES-256 Decryption: $decAes256 MB/s")
      
      decAes128 must beGreaterThan(10.0)
      decAes192 must beGreaterThan(10.0)
      decAes256 must beGreaterThan(10.0)
    }
  }
  
  def benchmark(algorithmName: String, fullAlgorithmName: String, mode: Int, keySize: Int, ivSize: Int): Double = {
    val key = new SecretKeySpec((1 to keySize).map(_.toByte).toArray, algorithmName)
    val iv = new IvParameterSpec((1 to ivSize).map(_.toByte).toArray)
      
    val encipher = Cipher.getInstance(fullAlgorithmName)
    encipher.init(mode, key, iv)
      
    val startTime = System.currentTimeMillis
    for (i <- 1 to iterations) {
      encipher.update(megaByteData)
    }
    encipher.doFinal()
    val endTime = System.currentTimeMillis
      
    // megabytes per second
    return (iterations * megaByteData.length).toDouble / (endTime - startTime).toDouble / 1024.0 / 1024.0 * 1000.0
  }
}