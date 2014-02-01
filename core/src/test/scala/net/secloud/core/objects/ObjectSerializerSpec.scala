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
import net.secloud.core.crypto._
import java.io.FileInputStream

@RunWith(classOf[JUnitRunner])
class ObjectSerializerSpec extends Specification {
  "ObjectSerializer" should {
    "serialize blobs" in {
      val content = "Hello World!"
      val key = AES.generate(32)

      val content1 = new ByteArrayInputStream(content.getBytes("ASCII"))
      val blob1 = Blob(ObjectId.empty)
      val intermediate1 = new ByteArrayOutputStream()
      BlobSerializer.write(intermediate1, blob1)
      BlobSerializer.writeContent(intermediate1, key)(cs => content1.pipeTo(cs))
      val intermediate2 = new ByteArrayInputStream(intermediate1.toByteArray)
      val content2 = new ByteArrayOutputStream()
      val blob2 = BlobSerializer.read(intermediate2)
      BlobSerializer.readContent(intermediate2, key)(cs => cs.pipeTo(content2))

      new String(content2.toByteArray, "ASCII") === content
    }

    "serialize trees" in {
      val key = AES.generate(32)

      val tree1 = Tree(ObjectId.empty, List(
        TreeEntry(ObjectId("000102"), FileTreeEntryMode, "test1", AES.generate(16)),
        TreeEntry(ObjectId("1231231212"), FileTreeEntryMode, "test2", AES.generate(24)),
        TreeEntry(ObjectId("00"), DirectoryTreeEntryMode, "test3", NullEncryption.generate(0))
      ))
      val intermediate1 = new ByteArrayOutputStream()
      TreeSerializer.write(intermediate1, tree1, key)
      val intermediate2 = new ByteArrayInputStream(intermediate1.toByteArray)
      val tree2 = TreeSerializer.read(intermediate2, key)

      tree1.entries.map(e => (e.id, e.name)) === tree2.entries.map(e => (e.id, e.name))
      tree1.entries.zip(tree2.entries).forall(e => compareSymmetricEncryptionKeys(e._1.key, e._2.key)) === true
    }

    "serialize commits" in {
      val key = AES.generate(32)
      val parents = List(CommitParent(ObjectId(), NullEncryption.generate(0)), CommitParent(ObjectId("00aaff"), AES.generate(16)))
      val tree = TreeEntry(ObjectId("ffee0011"), DirectoryTreeEntryMode, "", AES.generate(24))

      val commit1 = Commit(ObjectId.empty, parents, tree)
      val intermediate1 = new ByteArrayOutputStream()
      CommitSerializer.write(intermediate1, commit1, key)
      val intermediate2 = new ByteArrayInputStream(intermediate1.toByteArray)
      val commit2 = CommitSerializer.read(intermediate2, key)

      commit1.parents.map(_.id) === commit2.parents.map(_.id)
      commit1.parents.zip(commit2.parents).forall(p => compareSymmetricEncryptionKeys(p._1.key, p._2.key)) === true
      commit1.tree.id === commit2.tree.id
      compareSymmetricEncryptionKeys(commit1.tree.key, commit2.tree.key) === true
    }

    "sign and validate objects" in {
      val rsa = RSA.generate(512, 25)
      val bs1 = new ByteArrayOutputStream()
      ObjectSerializerCommons.signObject(bs1, rsa) { os =>
        os.writeString("Hello World")
      }

      val bs2 = new ByteArrayInputStream(bs1.toByteArray)
      ObjectSerializerCommons.validateObject(bs2, Map(RSA.fingerprint(rsa).toSeq -> rsa)) { is =>
        is.readString() === "Hello World"
      }

      ok
    }
  }

  def compareSymmetricEncryptionKeys(key1: SymmetricAlgorithmInstance, key2: SymmetricAlgorithmInstance): Boolean = {
    val ms1 = new ByteArrayOutputStream()
    key1.encrypt(ms1)(cs => cs.writeString("Hello World Foobar Buzzy"))

    val binary = ms1.toByteArray

    val ms2 = new ByteArrayInputStream(binary)
    key2.decrypt(ms2)(cs => cs.readString()) == "Hello World Foobar Buzzy"
  }
}
