package de.choffmeister.secloud.core

import org.specs2.mutable._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import de.choffmeister.secloud.core._
import de.choffmeister.secloud.core.ObjectSerializer._
import de.choffmeister.secloud.core.ObjectSerializerConstants._
import de.choffmeister.secloud.core.security.CryptographicAlgorithms._
import java.io.OutputStream
import java.io.InputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@RunWith(classOf[JUnitRunner])
class ObjectStreamSpec extends Specification {
  "ObjectStream" should {
    "only hash the header and the first three root blocks" in {
      val ms1 = new ByteArrayOutputStream()
      val os1 = new ObjectOutputStream(ms1, `SHA-1`)

      writeHeader(os1, CommitObjectType)
      writeIssuerIdentityBlock(os1, Issuer(Array[Byte](10, 20, 30), "me"))
      writeBlock(os1, PublicBlockType) { bs =>
        bs.write(0)
        bs.write(1)
      }
      writeBlock(os1, PrivateBlockType) { bs =>
        bs.write(10)
        bs.write(11)
      }

      val h1 = os1.hash
      h1 must beSome

      val ms2 = new ByteArrayOutputStream()
      val os2 = new ObjectOutputStream(ms2, `SHA-1`)

      writeHeader(os2, CommitObjectType)
      writeIssuerIdentityBlock(os2, Issuer(Array[Byte](10, 20, 30), "me"))
      writeBlock(os2, PublicBlockType) { bs =>
        bs.write(0)
        bs.write(1)
      }
      writeBlock(os2, PrivateBlockType) { bs =>
        bs.write(10)
        bs.write(11)
      }
      os2.write(Array[Byte](100, 101, 102))

      val h2 = os2.hash
      h2 must beSome

      h1 must beEqualTo(h2)

      val os3 = new ObjectInputStream(new ByteArrayInputStream(ms1.toByteArray), `SHA-1`)
      val os4 = new ObjectInputStream(new ByteArrayInputStream(ms2.toByteArray), `SHA-1`)

      readToEnd(os3)
      readToEnd(os4)

      os3.hash must beEqualTo(os4.hash)
    }
  }

  def readToEnd(stream: InputStream) {
    val buffer = new Array[Byte](1024)
    while (stream.read(buffer, 0, 1024) > 0) { }
  }
}