package net.secloud.core

import org.junit.runner.RunWith
import org.specs2.mutable._
import org.specs2.runner.JUnitRunner
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.io.InputStream
import java.io.ByteArrayInputStream
import net.secloud.core.security.CryptographicAlgorithms._
import java.io.FileInputStream

@RunWith(classOf[JUnitRunner])
class ObjectSerializerSpec extends Specification {
  "ObjectSerializer" should {
    "serialize blobs" in {
      val content = "Hello World!"
      val key = `AES-128`.generateKey()

      val content1 = new ByteArrayInputStream(content.getBytes("ASCII"))
      val blob1 = Blob(None, Issuer(Array[Byte](0, 1, -2, -1), "owner"))
      val intermediate1 = new ByteArrayOutputStream()
      Blob.write(intermediate1, blob1, content1, None, key)
      val intermediate2 = new ByteArrayInputStream(intermediate1.toByteArray)
      val content2 = new ByteArrayOutputStream()
      val blob2 = Blob.read(intermediate2, content2, key)

      blob1.issuer === blob2.issuer
      new String(content2.toByteArray, "ASCII") === content
    }

    "serialize blobs with known content size" in {
      val content = "Hello World! Foobar"
      val key = `AES-128`.generateKey()

      val content1 = new ByteArrayInputStream(content.getBytes("ASCII"))
      val blob1 = Blob(None, Issuer(Array[Byte](0, 1, -2, -1), "owner"))
      val intermediate1 = new ByteArrayOutputStream()
      Blob.write(intermediate1, blob1, content1, Some(content.length), key)
      val intermediate2 = new ByteArrayInputStream(intermediate1.toByteArray)
      val content2 = new ByteArrayOutputStream()
      val blob2 = Blob.read(intermediate2, content2, key)

      blob1.issuer === blob2.issuer
      new String(content2.toByteArray, "ASCII") === content
    }

    "serialize trees" in {
      val key = `AES-128`.generateKey()

      val tree1 = Tree(None, Issuer(Array[Byte](0, 1, -2, -1), "owner"))
      val intermediate1 = new ByteArrayOutputStream()
      Tree.write(intermediate1, tree1, key)
      val intermediate2 = new ByteArrayInputStream(intermediate1.toByteArray)
      val tree2 = Tree.read(intermediate2, key)

      tree2.issuer === tree2.issuer
    }

    "serialize commits" in {
      val key = `AES-128`.generateKey()

      val commit1 = Commit(None, Issuer(Array[Byte](0, 1, -2, -1), "owner"))
      val intermediate1 = new ByteArrayOutputStream()
      Commit.write(intermediate1, commit1, key)
      val intermediate2 = new ByteArrayInputStream(intermediate1.toByteArray)
      val commit2 = Commit.read(intermediate2, key)

      commit1.issuer === commit2.issuer
    }
  }
}
