package de.choffmeister.secloud.core

import org.junit.runner.RunWith
import org.specs2.mutable._
import org.specs2.runner.JUnitRunner
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.io.InputStream
import java.io.ByteArrayInputStream

@RunWith(classOf[JUnitRunner])
class ObjectSerializerSpec extends Specification {
  "ObjectSerializer" should {
    "serialize blobs" in {
      val algo = security.CryptographicAlgorithms.`AES-128`
      val params = algo.generateKey()
      val oid = ObjectId("0001efff")
      val issuer = Issuer(Array(0.toByte, 1.toByte, 128.toByte, 255.toByte), "me")
      val blob1 = Blob(oid, issuer)
      val buf = writeToBuffer(ObjectSerializer.serialize(blob1, _, algo, params))
      val blob2 = readFromBuffer(buf, ObjectSerializer.deserialize(oid, _, algo, params).asInstanceOf[Blob])

      blob1 === blob2
    }
  }

  def writeToBuffer(inner: OutputStream => Any): Array[Byte] = {
    val ms = new ByteArrayOutputStream()
    inner(ms)
    ms.toByteArray()
  }

  def readFromBuffer[T](buffer: Array[Byte], inner: InputStream => T): T = {
    val ms = new ByteArrayInputStream(buffer)
    inner(ms)
  }
}