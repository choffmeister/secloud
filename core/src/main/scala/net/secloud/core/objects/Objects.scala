package net.secloud.core.objects

import java.io.InputStream
import java.io.OutputStream
import net.secloud.core.objects.ObjectSerializer._
import net.secloud.core.objects.ObjectSerializerConstants._
import net.secloud.core.security.CryptographicAlgorithms._
import net.secloud.core.security.CryptographicAlgorithmSerializer._
import net.secloud.core.utils.RichStream._
import net.secloud.core.utils.BinaryReaderWriter._
import com.jcraft.jzlib.{GZIPInputStream, GZIPOutputStream}

sealed abstract class ObjectType
case object BlobObjectType extends ObjectType
case object TreeObjectType extends ObjectType
case object CommitObjectType extends ObjectType

case class Issuer(id: Seq[Byte], name: String)

sealed abstract class BaseObject {
  val id: ObjectId
  val issuer: Issuer
  val objectType: ObjectType
}

case class Blob(
  id: ObjectId,
  issuer: Issuer
) extends BaseObject {
  val objectType = BlobObjectType
}

sealed abstract class TreeEntryMode
case object NonExecutableFileTreeEntryMode extends TreeEntryMode
case object ExecutableFileTreeEntryMode extends TreeEntryMode
case object DirectoryTreeEntryMode extends TreeEntryMode

case class TreeEntry(
  id: ObjectId,
  mode: TreeEntryMode,
  name: String,
  key: SymmetricEncryptionParameters
)

case class Tree(
  id: ObjectId,
  issuer: Issuer,
  entries: List[TreeEntry]
) extends BaseObject {
  val objectType = TreeObjectType
}

case class Commit(
  id: ObjectId,
  issuer: Issuer,
  parentIds: List[ObjectId],
  treeId: ObjectId,
  treeKey: SymmetricEncryptionParameters
) extends BaseObject {
  val objectType = CommitObjectType
}

object Blob {
  def write(output: OutputStream, blob: Blob, content: InputStream, enc: SymmetricEncryptionParameters): Blob = {
    val ds = `SHA-2-256`.wrapStream(output)
    writeHeader(ds, blob.objectType)
    writeIssuerIdentityBlock(ds, blob.issuer)

    writePublicBlock(ds) { bs =>
    }

    writePrivateBlock(ds, enc) { bs =>
      val gzip = new GZIPOutputStream(bs)
      content.pipeTo(gzip)
      gzip.flush()
      gzip.close()
    }

    ds.flush()
    val digest = ds.getMessageDigest().digest.toSeq
    // TODO: sign hash
    writeIssuerSignatureBlock(output, digest)
    output.flush()
    return blob.copy(id = ObjectId(digest))
  }

  def read(input: InputStream, content: OutputStream, dec: SymmetricEncryptionParameters): Blob = {
    val ds = `SHA-2-256`.wrapStream(input)
    val objectType = readHeader(ds)
    assert("Expected blob", objectType == BlobObjectType)
    val issuer = readIssuerIdentityBlock(ds)

    readPublicBlock(ds) { bs =>
    }

    readPrivateBlock(ds, dec) { bs =>
      val gzip = new GZIPInputStream(bs)
      gzip.pipeTo(content)
      gzip.close()
    }

    val digest = ds.getMessageDigest.digest.toSeq
    val signature = readIssuerSignatureBlock(input)
    // TODO: validate signature with hash
    assert("Signature invalid", signature == digest)
    return Blob(ObjectId(digest), issuer)
  }
}

object Tree {
  def write(output: OutputStream, tree: Tree, enc: SymmetricEncryptionParameters): Tree = {
    val ds = `SHA-2-256`.wrapStream(output)
    writeHeader(ds, tree.objectType)
    writeIssuerIdentityBlock(ds, tree.issuer)

    writePublicBlock(ds) { bs =>
      bs.writeList(tree.entries) { e =>
        bs.writeObjectId(e.id)
        bs.writeInt8(treeEntryModeMap(e.mode))
      }
    }

    writePrivateBlock(ds, enc) { bs =>
      bs.writeList(tree.entries) { e =>
        bs.writeString(e.name)
        writeSymmetricEncryptionParameters(bs, e.key)
      }
    }

    ds.flush()
    val digest = ds.getMessageDigest().digest.toSeq
    // TODO: sign hash
    writeIssuerSignatureBlock(output, digest)
    return tree.copy(id = ObjectId(digest))
  }

  def read(input: InputStream, dec: SymmetricEncryptionParameters): Tree = {
    val ds = `SHA-2-256`.wrapStream(input)
    val objectType = readHeader(ds)
    assert("Expected tree", objectType == TreeObjectType)
    val issuer = readIssuerIdentityBlock(ds)

    val entryIdsAndModes = readPublicBlock(ds) { bs =>
      val entryIdsAndModes = bs.readList() {
        val id = bs.readObjectId()
        val mode = treeEntryModeMapInverse(bs.readInt8())
        (id, mode)
      }
      entryIdsAndModes
    }

    val entryNamesAndKey = readPrivateBlock(ds, dec) { bs =>
      val entryNamesAndKey = bs.readList() {
        (bs.readString(), readSymmetricEncryptionParameters(bs))
      }
      entryNamesAndKey
    }

    val entries = entryIdsAndModes.zip(entryNamesAndKey)
      .map(e => TreeEntry(e._1._1, e._1._2, e._2._1, e._2._2))
    val digest = ds.getMessageDigest().digest.toSeq
    val signature = readIssuerSignatureBlock(input)
    // TODO: validate signature with hash
    assert("Signature invalid", signature == digest)
    return Tree(ObjectId(digest), issuer, entries)
  }
}

object Commit {
  def write(output: OutputStream, commit: Commit, enc: SymmetricEncryptionParameters): Commit = {
    val ds = `SHA-2-256`.wrapStream(output)
    writeHeader(ds, commit.objectType)
    writeIssuerIdentityBlock(ds, commit.issuer)

    writePublicBlock(ds) { bs =>
      bs.writeList(commit.parentIds) {
        p => bs.writeObjectId(p)
      }
      bs.writeObjectId(commit.treeId)
    }

    writePrivateBlock(ds, enc) { bs =>
      writeSymmetricEncryptionParameters(bs, commit.treeKey)
    }

    ds.flush()
    val digest = ds.getMessageDigest().digest.toSeq
    // TODO: sign hash
    writeIssuerSignatureBlock(output, digest)
    return commit.copy(id = ObjectId(digest))
  }

  def read(input: InputStream, dec: SymmetricEncryptionParameters): Commit = {
    val ds = `SHA-2-256`.wrapStream(input)
    val objectType = readHeader(ds)
    assert("Expected commit", objectType == CommitObjectType)
    val issuer = readIssuerIdentityBlock(ds)

    val (parentIds, treeId) = readPublicBlock(ds) { bs =>
      val parentIds = bs.readList() {
        bs.readObjectId()
      }
      val treeId = bs.readObjectId()
      (parentIds, treeId)
    }

    val treeKey = readPrivateBlock(ds, dec) { bs =>
      val treeKey = readSymmetricEncryptionParameters(bs)
      treeKey
    }

    val digest = ds.getMessageDigest().digest.toSeq
    val signature = readIssuerSignatureBlock(input)
    // TODO: validate signature with hash
    assert("Signature invalid", signature == digest)
    return Commit(ObjectId(digest), issuer, parentIds, treeId, treeKey)
  }
}
