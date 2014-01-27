package net.secloud.core.security

import org.specs2.mutable._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import net.secloud.core.utils.BinaryReaderWriter._
import net.secloud.core.security.CryptographicAlgorithms._
import net.secloud.core.security.CryptographicAlgorithmSerializer._
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import java.io.InputStream
import javax.crypto.SecretKey
import org.apache.commons.codec.binary.Hex

@RunWith(classOf[JUnitRunner])
class CryptographicAlgorithmsSpec extends Specification {
  "SymmetricAlgorithm" should {
    "wrap streams with AES-128" in {
      testForConcreteAlgorithm(`AES-128`, "") === ""
      testForConcreteAlgorithm(`AES-128`, "Hello") === "Hello"
      testForConcreteAlgorithm(`AES-128`, "Hello World!") === "Hello World!"
      success
    }

    "wrap streams with AES-192" in {
      if (`AES-192`.supported) {
        testForConcreteAlgorithm(`AES-192`, "") === ""
        testForConcreteAlgorithm(`AES-192`, "Hello") === "Hello"
        testForConcreteAlgorithm(`AES-192`, "Hello World!") === "Hello World!"
        success
      } else skipped("AES-192 is not supported")
    }

    "wrap streams with AES-256" in {
      if (`AES-256`.supported) {
        testForConcreteAlgorithm(`AES-256`, "") === ""
        testForConcreteAlgorithm(`AES-256`, "Hello") === "Hello"
        testForConcreteAlgorithm(`AES-256`, "Hello World!") === "Hello World!"
        success
      } else skipped("AES-256 is not supported")
    }
  }

  "CryptographicAlgorithmSerializer" should {
    "serialize and deserialize AES-128" in {
      testForConcreteAlgorithmWithSerialization(`AES-128`, "") === ""
      testForConcreteAlgorithmWithSerialization(`AES-128`, "Hello") === "Hello"
      testForConcreteAlgorithmWithSerialization(`AES-128`, "Hello World!") === "Hello World!"
      success
    }

    "serialize and deserialize AES-192" in {
      if (`AES-192`.supported) {
        testForConcreteAlgorithmWithSerialization(`AES-192`, "") === ""
        testForConcreteAlgorithmWithSerialization(`AES-192`, "Hello") === "Hello"
        testForConcreteAlgorithmWithSerialization(`AES-192`, "Hello World!") === "Hello World!"
        success
      } else skipped("AES-192 is not supported")
    }

    "serialize and deserialize AES-256" in {
      if (`AES-256`.supported) {
        testForConcreteAlgorithmWithSerialization(`AES-256`, "") === ""
        testForConcreteAlgorithmWithSerialization(`AES-256`, "Hello") === "Hello"
        testForConcreteAlgorithmWithSerialization(`AES-256`, "Hello World!") === "Hello World!"
        success
      } else skipped("AES-256 is not supported")
    }
  }

  def testForConcreteAlgorithm(algorithm: SymmetricAlgorithm, text: String): String = {
    val key = algorithm.generateParameters()
    val ms1 = new ByteArrayOutputStream()
    val cs1 = algorithm.wrapStream(ms1, key)
    cs1.writeString(text)
    cs1.close()

    val binary = ms1.toByteArray

    val ms2 = new ByteArrayInputStream(binary)
    val cs2 = algorithm.wrapStream(ms2, key)

    cs2.readString()
  }

  def testForConcreteAlgorithmWithSerialization(algorithm: SymmetricAlgorithm, text: String): String = {
    val key1 = algorithm.generateParameters()
    val ms1 = new ByteArrayOutputStream()
    val cs1 = algorithm.wrapStream(ms1, key1)
    cs1.writeString(text)
    cs1.close()

    val binary = ms1.toByteArray

    val keyStreamOut = new ByteArrayOutputStream()
    writeSymmetricParams(keyStreamOut, key1)
    val keyStreamIn = new ByteArrayInputStream(keyStreamOut.toByteArray)

    val key2 = readSymmetricParams(keyStreamIn)
    val ms2 = new ByteArrayInputStream(binary)
    val cs2 = algorithm.wrapStream(ms2, key2)

    cs2.readString()
  }
}
