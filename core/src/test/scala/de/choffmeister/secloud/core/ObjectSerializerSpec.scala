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
      val issuer = Issuer(Array(0.toByte, 1.toByte, 128.toByte, 255.toByte), "me")
      val blob1 = Blob(oid, issuer)
      val buf = writeToBuffer(ObjectSerializer.serialize(blob1, _))
      val blob2 = readFromBuffer(buf, ObjectSerializer.deserialize(oid, _).asInstanceOf[Blob])

      blob1.id === blob2.id
      blob1.issuer.id === blob2.issuer.id
      blob1.issuer.name === blob2.issuer.name
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