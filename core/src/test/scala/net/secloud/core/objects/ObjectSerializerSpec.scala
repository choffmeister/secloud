package net.secloud.core.objects

import org.junit.runner.RunWith
import org.specs2.mutable._
import org.specs2.runner.JUnitRunner
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.io.InputStream
import java.io.ByteArrayInputStream
import net.secloud.core.utils.BinaryReaderWriter._
import net.secloud.core.security.CryptographicAlgorithms._
import java.io.FileInputStream

@RunWith(classOf[JUnitRunner])
class ObjectSerializerSpec extends Specification {
  "ObjectSerializer" should {
    "serialize blobs" in {
      val content = "Hello World!"
      val key = `AES-128`.generateKey()

      val content1 = new ByteArrayInputStream(content.getBytes("ASCII"))
      val blob1 = Blob(ObjectId.empty, Issuer(Array[Byte](0, 1, -2, -1), "owner"))
      val intermediate1 = new ByteArrayOutputStream()
      BlobSerializer.write(intermediate1, blob1, content1, key)
      val intermediate2 = new ByteArrayInputStream(intermediate1.toByteArray)
      val content2 = new ByteArrayOutputStream()
      val blob2 = BlobSerializer.read(intermediate2, content2, key)

      blob1.issuer === blob2.issuer
      new String(content2.toByteArray, "ASCII") === content
    }

    "serialize trees" in {
      val key = `AES-128`.generateKey()

      val tree1 = Tree(ObjectId.empty, Issuer(Array[Byte](0, 1, -2, -1), "owner"), List(
        TreeEntry(ObjectId("000102"), NonExecutableFileTreeEntryMode, "test1", `AES-128`.generateKey()),
        TreeEntry(ObjectId("1231231212"), ExecutableFileTreeEntryMode, "test2", `AES-128`.generateKey()),
        TreeEntry(ObjectId("00"), DirectoryTreeEntryMode, "test3", NullEncryption.generateKey())
      ))
      val intermediate1 = new ByteArrayOutputStream()
      TreeSerializer.write(intermediate1, tree1, key)
      val intermediate2 = new ByteArrayInputStream(intermediate1.toByteArray)
      val tree2 = TreeSerializer.read(intermediate2, key)

      tree1.issuer === tree2.issuer
      tree1.entries.map(e => (e.id, e.name)) === tree2.entries.map(e => (e.id, e.name))
      tree1.entries.zip(tree2.entries).forall(e => compareSymmetricEncryptionKeys(e._1.key, e._2.key)) === true
    }

    "serialize commits" in {
      val key = `AES-128`.generateKey()

      val commit1 = Commit(ObjectId.empty, Issuer(Array[Byte](0, 1, -2, -1), "owner"), List(ObjectId(), ObjectId("00aaff")), ObjectId("0011feff"), `AES-128`.generateKey())
      val intermediate1 = new ByteArrayOutputStream()
      CommitSerializer.write(intermediate1, commit1, key)
      val intermediate2 = new ByteArrayInputStream(intermediate1.toByteArray)
      val commit2 = CommitSerializer.read(intermediate2, key)

      commit1.issuer === commit2.issuer
      commit1.parentIds === commit2.parentIds
      commit1.treeId === commit2.treeId
      compareSymmetricEncryptionKeys(commit1.treeKey, commit2.treeKey) === true
    }
  }

  def compareSymmetricEncryptionKeys(key1: SymmetricEncryptionParameters, key2: SymmetricEncryptionParameters): Boolean = {
    val ms1 = new ByteArrayOutputStream()
    val cs1 = key1.algorithm.wrapStream(ms1, key1)
    cs1.writeString("Hello World Foobar Buzzy")
    cs1.close()

    val binary = ms1.toByteArray

    val ms2 = new ByteArrayInputStream(binary)
    val cs2 = key2.algorithm.wrapStream(ms2, key2)

    return cs2.readString() == "Hello World Foobar Buzzy"
  }
}
