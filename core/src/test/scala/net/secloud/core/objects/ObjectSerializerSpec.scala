package net.secloud.core.objects

import org.junit.runner.RunWith
import org.specs2.mutable._
import org.specs2.runner.JUnitRunner
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.io.InputStream
import java.io.ByteArrayInputStream
import net.secloud.core.utils.BinaryReaderWriter._
import net.secloud.core.utils.RichStream._
import net.secloud.core.security.CryptographicAlgorithms._
import java.io.FileInputStream

@RunWith(classOf[JUnitRunner])
class ObjectSerializerSpec extends Specification {
  "ObjectSerializer" should {
    "serialize blobs" in {
      val content = "Hello World!"
      val key = `AES-128`.generateParameters()

      val content1 = new ByteArrayInputStream(content.getBytes("ASCII"))
      val blob1 = Blob(ObjectId.empty, Issuer(Array[Byte](0, 1, -2, -1), "owner"))
      val intermediate1 = new ByteArrayOutputStream()
      BlobSerializer.write(intermediate1, blob1)
      BlobSerializer.writeContent(intermediate1, key)(cs => content1.pipeTo(cs))
      val intermediate2 = new ByteArrayInputStream(intermediate1.toByteArray)
      val content2 = new ByteArrayOutputStream()
      val blob2 = BlobSerializer.read(intermediate2)
      BlobSerializer.readContent(intermediate2, key)(cs => cs.pipeTo(content2))

      blob1.issuer === blob2.issuer
      new String(content2.toByteArray, "ASCII") === content
    }

    "serialize trees" in {
      val key = `AES-128`.generateParameters()

      val tree1 = Tree(ObjectId.empty, Issuer(Array[Byte](0, 1, -2, -1), "owner"), List(
        TreeEntry(ObjectId("000102"), NonExecutableFileTreeEntryMode, "test1", `AES-128`.generateParameters()),
        TreeEntry(ObjectId("1231231212"), ExecutableFileTreeEntryMode, "test2", `AES-128`.generateParameters()),
        TreeEntry(ObjectId("00"), DirectoryTreeEntryMode, "test3", NullEncryption.generateParameters())
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
      val key = `AES-128`.generateParameters()
      val parents = List(CommitParent(ObjectId(), NullEncryption.generateParameters()), CommitParent(ObjectId("00aaff"), `AES-128`.generateParameters()))
      val tree = TreeEntry(ObjectId("ffee0011"), DirectoryTreeEntryMode, "", `AES-128`.generateParameters())

      val commit1 = Commit(ObjectId.empty, Issuer(Array[Byte](0, 1, -2, -1), "owner"), parents, tree)
      val intermediate1 = new ByteArrayOutputStream()
      CommitSerializer.write(intermediate1, commit1, key)
      val intermediate2 = new ByteArrayInputStream(intermediate1.toByteArray)
      val commit2 = CommitSerializer.read(intermediate2, key)

      commit1.issuer === commit2.issuer
      commit1.parents.map(_.id) === commit2.parents.map(_.id)
      commit1.parents.zip(commit2.parents).forall(p => compareSymmetricEncryptionKeys(p._1.key, p._2.key)) === true
      commit1.tree.id === commit2.tree.id
      compareSymmetricEncryptionKeys(commit1.tree.key, commit2.tree.key) === true
    }
  }

  def compareSymmetricEncryptionKeys(key1: SymmetricParams, key2: SymmetricParams): Boolean = {
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
