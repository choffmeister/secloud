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
class ObjectHashStreamSpec extends Specification {
  "ObjectHashStream" should {
    "only hash the header and the first three root blocks" in {
      val hashAlgo = `SHA-1`
      val ms1 = new ByteArrayOutputStream()
      val os1 = new ObjectHashOutputStream(ms1, hashAlgo)

      writeHeader(os1, CommitObjectType)
      writeIssuerIdentityBlock(os1, Issuer(Array[Byte](Byte.MinValue, 127, Byte.MaxValue), "me"))
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
      val os2 = new ObjectHashOutputStream(ms2, hashAlgo)

      writeHeader(os2, CommitObjectType)
      writeIssuerIdentityBlock(os2, Issuer(Array[Byte](Byte.MinValue, 127, Byte.MaxValue), "me"))
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

      val os3 = new ObjectHashInputStream(new ByteArrayInputStream(ms1.toByteArray), hashAlgo)
      val os4 = new ObjectHashInputStream(new ByteArrayInputStream(ms2.toByteArray), hashAlgo)

      readToEnd(os3)
      readToEnd(os4)

      val h3 = os3.hash
      val h4 = os4.hash
      
      h1 === h2
      h2 === h3
      h3 === h4
      h4 === h1
    }

    "only hash the header and the first three root blocks (empty pub and priv block)" in {
      val hashAlgo = `SHA-1`
      val ms1 = new ByteArrayOutputStream()
      val os1 = new ObjectHashOutputStream(ms1, hashAlgo)

      writeHeader(os1, CommitObjectType)
      writeIssuerIdentityBlock(os1, Issuer(Array[Byte](Byte.MinValue, 127, Byte.MaxValue), "me"))
      writeBlock(os1, PublicBlockType) { bs =>
      }
      writeBlock(os1, PrivateBlockType) { bs =>
      }

      val h1 = os1.hash
      h1 must beSome

      val ms2 = new ByteArrayOutputStream()
      val os2 = new ObjectHashOutputStream(ms2, hashAlgo)

      writeHeader(os2, CommitObjectType)
      writeIssuerIdentityBlock(os2, Issuer(Array[Byte](Byte.MinValue, 127, Byte.MaxValue), "me"))
      writeBlock(os2, PublicBlockType) { bs =>
      }
      writeBlock(os2, PrivateBlockType) { bs =>
      }
      os2.write(Array[Byte](100, 101, 102))

      val h2 = os2.hash
      h2 must beSome

      h1 must beEqualTo(h2)

      val os3 = new ObjectHashInputStream(new ByteArrayInputStream(ms1.toByteArray), hashAlgo)
      val os4 = new ObjectHashInputStream(new ByteArrayInputStream(ms2.toByteArray), hashAlgo)

      readToEnd(os3)
      readToEnd(os4)

      val h3 = os3.hash
      val h4 = os4.hash
      
      h1 === h2
      h2 === h3
      h3 === h4
      h4 === h1
    }
  }

  def readToEnd(stream: InputStream) {
    val buffer = new Array[Byte](1024)
    while (stream.read(buffer, 0, 1024) > 0) { }
  }
}