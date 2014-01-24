package net.secloud.core.objects

import java.io.InputStream
import java.io.OutputStream
import net.secloud.core.objects.ObjectSerializerConstants._
import net.secloud.core.objects.ObjectSerializerCommons._
import net.secloud.core.security.CryptographicAlgorithms._
import net.secloud.core.security.CryptographicAlgorithmSerializer._
import net.secloud.core.utils.RichStream._
import net.secloud.core.utils.BinaryReaderWriter._
import com.jcraft.jzlib.{GZIPInputStream, GZIPOutputStream}

private[objects] object BlobSerializer {
  def write(output: OutputStream, blob: Blob, content: InputStream, enc: SymmetricEncryptionParameters): Blob = {
    val ds = `SHA-2-256`.wrapStream(output)
    writeHeader(ds, blob.objectType)
    writeIssuerIdentityBlock(ds, blob.issuer)

    writePublicBlock(ds) { bs =>
    }

    writePrivateBlock(ds, enc) { bs =>
      writeCompressed(bs) { cs =>
        content.pipeTo(cs)
      }
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
      readCompressed(bs) { cs =>
        cs.pipeTo(content)
      }
    }

    val digest = ds.getMessageDigest.digest.toSeq
    val signature = readIssuerSignatureBlock(input)
    // TODO: validate signature with hash
    assert("Signature invalid", signature == digest)
    return Blob(ObjectId(digest), issuer)
  }
}

private[objects] object TreeSerializer {
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

private[objects] object CommitSerializer {
  def write(output: OutputStream, commit: Commit, enc: SymmetricEncryptionParameters): Commit = {
    val ds = `SHA-2-256`.wrapStream(output)
    writeHeader(ds, commit.objectType)
    writeIssuerIdentityBlock(ds, commit.issuer)

    writePublicBlock(ds) { bs =>
      bs.writeList(commit.parents) {
        p => bs.writeObjectId(p.id)
      }
      bs.writeObjectId(commit.tree.id)
    }

    writePrivateBlock(ds, enc) { bs =>
      bs.writeList(commit.parents) {
        p => writeSymmetricEncryptionParameters(bs, p.key)
      }
      writeSymmetricEncryptionParameters(bs, commit.tree.key)
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

    val (parentKeys, treeKey) = readPrivateBlock(ds, dec) { bs =>
      val parentIds = bs.readList() {
        readSymmetricEncryptionParameters(bs)
      }
      val treeKey = readSymmetricEncryptionParameters(bs)
      (parentIds, treeKey)
    }

    val parents = parentIds.zip(parentKeys)
      .map(p => CommitParent(p._1, p._2))
    val tree = TreeEntry(treeId, DirectoryTreeEntryMode, "", treeKey)
    val digest = ds.getMessageDigest().digest.toSeq
    val signature = readIssuerSignatureBlock(input)
    // TODO: validate signature with hash
    assert("Signature invalid", signature == digest)
    return Commit(ObjectId(digest), issuer, parents, tree)
  }
}
