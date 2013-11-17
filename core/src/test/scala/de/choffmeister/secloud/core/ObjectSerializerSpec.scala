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
      val oid = ObjectId("0001efff")
      val issuer = IssuerInformation(
       Array(0.toByte, 1.toByte, 128.toByte, 255.toByte),
        "me",
        Array(10.toByte, 56.toByte, 241.toByte, 56.toByte)
      )
      val blob1 = Blob(oid, issuer)
      val buf = writeToBuffer(ObjectSerializer.serialize(blob1, _))
      val blob2 = readFromBuffer(buf, ObjectSerializer.deserialize(_).asInstanceOf[Blob])

      blob1.id === blob2.id
      blob1.issuer.id === blob2.issuer.id
      blob1.issuer.name === blob2.issuer.name
      blob1.issuer.signature === blob2.issuer.signature
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