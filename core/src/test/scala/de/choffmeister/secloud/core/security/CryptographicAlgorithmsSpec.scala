package de.choffmeister.secloud.core.security

import org.specs2.mutable._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import de.choffmeister.secloud.core.utils.BinaryReaderWriter._
import de.choffmeister.secloud.core.security.CryptographicAlgorithms._
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import java.io.InputStream
import javax.crypto.SecretKey
import org.apache.commons.codec.binary.Hex

@RunWith(classOf[JUnitRunner])
class CryptographicAlgorithmsSpec extends Specification {
  "SymmetricEncryptionAlgorithm" should {
    "wrap streams with AES-128" in {
      if (`AES-128`.supported) {
        testForConcreteAlgorithm(`AES-128`, "") === ""
        testForConcreteAlgorithm(`AES-128`, "Hello") === "Hello"
        testForConcreteAlgorithm(`AES-128`, "Hello World!") === "Hello World!"
        success
      } else skipped("AES-128 is not supported")
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

  def testForConcreteAlgorithm(algorithm: SymmetricEncryptionAlgorithm, text: String): String = {
    val key1 = algorithm.generateKey()
    val ms1 = new ByteArrayOutputStream()
    val cs1 = algorithm.wrapStream(ms1, key1)
    cs1.writeString(text)
    cs1.close()

    val binary = ms1.toByteArray

    val key2 = serializeAndDeserializeParameters(algorithm, key1)
    val ms2 = new ByteArrayInputStream(binary)
    val cs2 = algorithm.wrapStream(ms2, key2)

    cs2.readString()
  }
  
  def serializeAndDeserializeParameters(algorithm: SymmetricEncryptionAlgorithm, key: SymmetricEncryptionParameters): SymmetricEncryptionParameters = {
    val out = new ByteArrayOutputStream()
    algorithm.writeParameters(out, key)
    val in = new ByteArrayInputStream(out.toByteArray)
    algorithm.readParameters(in)
  }
}