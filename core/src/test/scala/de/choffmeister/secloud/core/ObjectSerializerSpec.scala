package de.choffmeister.secloud.core

import org.junit.runner.RunWith
import org.specs2.mutable._
import org.specs2.runner.JUnitRunner

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
      val buf = ObjectSerializer.serialize(blob1)
      val blob2 = ObjectSerializer.deserialize(buf).asInstanceOf[Blob]

      blob1.id === blob2.id
      blob1.issuer.id === blob2.issuer.id
      blob1.issuer.name === blob2.issuer.name
      blob1.issuer.signature === blob2.issuer.signature
    }
  }
}