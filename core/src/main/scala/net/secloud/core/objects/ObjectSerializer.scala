package net.secloud.core.objects

import java.io.InputStream
import java.io.OutputStream
import net.secloud.core.objects.ObjectSerializerConstants._
import net.secloud.core.objects.ObjectSerializerCommons._
import net.secloud.core.crypto._
import net.secloud.core.utils._
import net.secloud.core.utils.StreamUtils._

object ObjectSerializer {
  def writeBlob(output: OutputStream, blob: Blob): Unit =
    BlobSerializer.write(output, blob)
  def readBlob(input: InputStream): Blob =
    BlobSerializer.read(input)
  def writeBlobContent(output: OutputStream, key: SymmetricAlgorithmInstance)(inner: OutputStream ⇒ Any): Unit =
    BlobSerializer.writeContent(output, key)(inner)
  def readBlobContent[T](input: InputStream, key: SymmetricAlgorithmInstance)(inner: InputStream ⇒ T): T =
    BlobSerializer.readContent(input, key)(inner)

  def writeTree(output: OutputStream, tree: Tree, key: SymmetricAlgorithmInstance): Unit =
    TreeSerializer.write(output, tree, key)
  def readTree(input: InputStream, key: SymmetricAlgorithmInstance): Tree =
    TreeSerializer.read(input, key)

  def writeCommit(output: OutputStream, commit: Commit, key: SymmetricAlgorithmInstance): Unit =
    CommitSerializer.write(output, commit, key)
  def readCommit(input: InputStream, key: Either[SymmetricAlgorithmInstance, AsymmetricAlgorithmInstance]): Commit =
    CommitSerializer.read(input, key)

  def signObject(output: OutputStream, privateKey: AsymmetricAlgorithmInstance)(inner: OutputStream ⇒ Any): ObjectId =
    ObjectSerializerCommons.signObject(output, privateKey)(inner)
  def validateObject[T](input: InputStream, publicKeys: Map[Seq[Byte], AsymmetricAlgorithmInstance])(inner: InputStream ⇒ T): T =
    ObjectSerializerCommons.validateObject(input, publicKeys)(inner)
}

private[objects] object BlobSerializer {
  def write(output: OutputStream, blob: Blob): Unit = {
    writeHeader(output, blob.objectType)

    writePublicBlock(output) { bs ⇒
    }

    output.flush()
  }

  def read(input: InputStream): Blob = {
    val objectType = readHeader(input)
    assert("Expected blob", objectType == BlobObjectType)

    readPublicBlock(input) { bs ⇒
    }

    return Blob(ObjectId())
  }

  def writeContent(output: OutputStream, key: SymmetricAlgorithmInstance)(inner: OutputStream ⇒ Any): Unit = {
    writePrivateBlock(output, key) { bs ⇒
      writeCompressed(bs) { cs ⇒
        inner(cs)
      }
    }

    output.flush()
  }

  def readContent[T](input: InputStream, key: SymmetricAlgorithmInstance)(inner: InputStream ⇒ T): T = {
    readPrivateBlock(input, key) { bs ⇒
      readCompressed(bs) { cs ⇒
        inner(cs)
      }
    }
  }
}

private[objects] object TreeSerializer {
  def write(output: OutputStream, tree: Tree, key: SymmetricAlgorithmInstance): Unit = {
    writeHeader(output, tree.objectType)

    writePublicBlock(output) { bs ⇒
      bs.writeList(tree.entries) { e ⇒
        bs.writeObjectId(e.id)
        bs.writeInt8(treeEntryModeMap(e.mode))
      }
    }

    writePrivateBlock(output, key) { bs ⇒
      bs.writeList(tree.entries) { e ⇒
        bs.writeString(e.name)
        writeSymmetricAlgorithm(bs, e.key)
        bs.writeBinary(e.hash)
      }
    }

    output.flush()
  }

  def read(input: InputStream, key: SymmetricAlgorithmInstance): Tree = {
    val objectType = readHeader(input)
    assert("Expected tree", objectType == TreeObjectType)

    val entryIdsAndModes = readPublicBlock(input) { bs ⇒
      bs.readList() {
        val id = bs.readObjectId()
        val mode = treeEntryModeMapInverse(bs.readInt8())
        (id, mode)
      }
    }

    val entryNamesKeyAndHash = readPrivateBlock(input, key) { bs ⇒
      bs.readList() {
        val name = bs.readString()
        val key = readSymmetricAlgorithm(bs)
        val hash = bs.readBinary().toSeq
        (name, key, hash)
      }
    }

    val entries = entryIdsAndModes.zip(entryNamesKeyAndHash)
      .map(e ⇒ TreeEntry(e._1._1, e._1._2, e._2._1, e._2._2, e._2._3))

    return Tree(ObjectId(), entries)
  }
}

private[objects] object CommitSerializer {
  def write(output: OutputStream, commit: Commit, key: SymmetricAlgorithmInstance): Unit = {
    writeHeader(output, commit.objectType)

    writePublicBlock(output) { bs ⇒
      bs.writeList(commit.parentIds)(id ⇒ bs.writeObjectId(id))
      bs.writeMap(commit.issuers) { (fingerprint, issuer) ⇒
        bs.writeBinary(fingerprint)
        bs.writeString(issuer.name)
        writeAsymmetricAlgorithm(bs, issuer.publicKey, false)
      }
      val keyEncoded = streamAsBytes(s ⇒ writeSymmetricAlgorithm(s, key))
      val encapsulatedCommitKeys = commit.issuers.map(i ⇒ (i._1, i._2.publicKey.wrapKey(keyEncoded).toSeq))
      bs.writeMap(encapsulatedCommitKeys) { (issuerFingerprint, encapsulatedKey) ⇒
        bs.writeBinary(issuerFingerprint)
        bs.writeBinary(encapsulatedKey)
      }
      bs.writeObjectId(commit.tree.id)
    }

    writePrivateBlock(output, key) { bs ⇒
      writeSymmetricAlgorithm(bs, commit.tree.key)
    }

    output.flush()
  }

  def read(input: InputStream, key: Either[SymmetricAlgorithmInstance, AsymmetricAlgorithmInstance]): Commit = {
    val objectType = readHeader(input)
    assert("Expected commit", objectType == CommitObjectType)

    val (parentIds, issuers, encapsulatedCommitKeys, treeId) = readPublicBlock(input) { bs ⇒
      val parentIds = bs.readList()(bs.readObjectId())
      val issuers = bs.readMap() {
        val fingerprint = bs.readBinary().toSeq
        val name = bs.readString()
        val publicKey = readAsymmetricAlgorithm(bs)
        (fingerprint, Issuer(name, publicKey))
      }
      val encapsulatedCommitKeys = bs.readMap() {
        val issuerFingerprint = bs.readBinary().toSeq
        val encapsulatedCommitKey = bs.readBinary().toSeq
        (issuerFingerprint, encapsulatedCommitKey)
      }
      val treeId = bs.readObjectId()
      (parentIds, issuers, encapsulatedCommitKeys, treeId)
    }

    val commitKey = key match {
      case Left(sk) ⇒ sk
      case Right(apk) ⇒
        val encapsulatedCommitKey = encapsulatedCommitKeys(apk.fingerprint.toSeq).toArray
        bytesAsStream(apk.unwrapKey(encapsulatedCommitKey))(s ⇒ readSymmetricAlgorithm(s))
    }
    val treeKey = readPrivateBlock(input, commitKey) { bs ⇒
      readSymmetricAlgorithm(bs)
    }

    val tree = TreeEntry(treeId, DirectoryTreeEntryMode, "", treeKey)

    return Commit(ObjectId(), parentIds, issuers, encapsulatedCommitKeys, tree)
  }
}
