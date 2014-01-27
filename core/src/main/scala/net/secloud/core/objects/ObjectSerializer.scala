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
  def write(output: OutputStream, blob: Blob, content: InputStream, enc: SymmetricEncryptionParameters): Unit = {
    writeHeader(output, blob.objectType)
    writeIssuerIdentityBlock(output, blob.issuer)

    writePublicBlock(output) { bs =>
    }

    writePrivateBlock(output, enc) { bs =>
      writeCompressed(bs) { cs =>
        content.pipeTo(cs)
      }
    }

    output.flush()
  }

  def read(input: InputStream, content: OutputStream, dec: SymmetricEncryptionParameters): Blob = {
    val objectType = readHeader(input)
    assert("Expected blob", objectType == BlobObjectType)
    val issuer = readIssuerIdentityBlock(input)

    readPublicBlock(input) { bs =>
    }

    readPrivateBlock(input, dec) { bs =>
      readCompressed(bs) { cs =>
        cs.pipeTo(content)
      }
    }

    return Blob(ObjectId(), issuer)
  }
}

private[objects] object TreeSerializer {
  def write(output: OutputStream, tree: Tree, enc: SymmetricEncryptionParameters): Unit = {
    writeHeader(output, tree.objectType)
    writeIssuerIdentityBlock(output, tree.issuer)

    writePublicBlock(output) { bs =>
      bs.writeList(tree.entries) { e =>
        bs.writeObjectId(e.id)
        bs.writeInt8(treeEntryModeMap(e.mode))
      }
    }

    writePrivateBlock(output, enc) { bs =>
      bs.writeList(tree.entries) { e =>
        bs.writeString(e.name)
        writeSymmetricEncryptionParameters(bs, e.key)
      }
    }

    output.flush()
  }

  def read(input: InputStream, dec: SymmetricEncryptionParameters): Tree = {
    val objectType = readHeader(input)
    assert("Expected tree", objectType == TreeObjectType)
    val issuer = readIssuerIdentityBlock(input)

    val entryIdsAndModes = readPublicBlock(input) { bs =>
      val entryIdsAndModes = bs.readList() {
        val id = bs.readObjectId()
        val mode = treeEntryModeMapInverse(bs.readInt8())
        (id, mode)
      }
      entryIdsAndModes
    }

    val entryNamesAndKey = readPrivateBlock(input, dec) { bs =>
      val entryNamesAndKey = bs.readList() {
        (bs.readString(), readSymmetricEncryptionParameters(bs))
      }
      entryNamesAndKey
    }

    val entries = entryIdsAndModes.zip(entryNamesAndKey)
      .map(e => TreeEntry(e._1._1, e._1._2, e._2._1, e._2._2))

    return Tree(ObjectId(), issuer, entries)
  }
}

private[objects] object CommitSerializer {
  def write(output: OutputStream, commit: Commit, enc: SymmetricEncryptionParameters): Unit = {
    writeHeader(output, commit.objectType)
    writeIssuerIdentityBlock(output, commit.issuer)

    writePublicBlock(output) { bs =>
      bs.writeList(commit.parents) {
        p => bs.writeObjectId(p.id)
      }
      bs.writeObjectId(commit.tree.id)
    }

    writePrivateBlock(output, enc) { bs =>
      bs.writeList(commit.parents) {
        p => writeSymmetricEncryptionParameters(bs, p.key)
      }
      writeSymmetricEncryptionParameters(bs, commit.tree.key)
    }

    output.flush()
  }

  def read(input: InputStream, dec: SymmetricEncryptionParameters): Commit = {
    val objectType = readHeader(input)
    assert("Expected commit", objectType == CommitObjectType)
    val issuer = readIssuerIdentityBlock(input)

    val (parentIds, treeId) = readPublicBlock(input) { bs =>
      val parentIds = bs.readList() {
        bs.readObjectId()
      }
      val treeId = bs.readObjectId()
      (parentIds, treeId)
    }

    val (parentKeys, treeKey) = readPrivateBlock(input, dec) { bs =>
      val parentIds = bs.readList() {
        readSymmetricEncryptionParameters(bs)
      }
      val treeKey = readSymmetricEncryptionParameters(bs)
      (parentIds, treeKey)
    }

    val parents = parentIds.zip(parentKeys)
      .map(p => CommitParent(p._1, p._2))
    val tree = TreeEntry(treeId, DirectoryTreeEntryMode, "", treeKey)
    
    return Commit(ObjectId(), issuer, parents, tree)
  }
}
