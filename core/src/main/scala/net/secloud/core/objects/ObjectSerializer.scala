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
  def write(output: OutputStream, blob: Blob): Unit = {
    writeHeader(output, blob.objectType)
    writeIssuerIdentityBlock(output, blob.issuer)

    writePublicBlock(output) { bs =>
    }

    output.flush()
  }

  def read(input: InputStream): Blob = {
    val objectType = readHeader(input)
    assert("Expected blob", objectType == BlobObjectType)
    val issuer = readIssuerIdentityBlock(input)

    readPublicBlock(input) { bs =>
    }

    return Blob(ObjectId(), issuer)
  }

  def writeContent(output: OutputStream, enc: SymmetricParams)(inner: OutputStream => Any): Unit = {
    writePrivateBlock(output, enc) { bs =>
      writeCompressed(bs) { cs =>
        inner(cs)
      }
    }

    output.flush()
  }

  def readContent[T](input: InputStream, dec: SymmetricParams)(inner: InputStream => T): T = {
    readPrivateBlock(input, dec) { bs =>
      readCompressed(bs) { cs =>
        inner(cs)
      }
    }
  }
}

private[objects] object TreeSerializer {
  def write(output: OutputStream, tree: Tree, enc: SymmetricParams): Unit = {
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
        writeSymmetricParams(bs, e.key)
      }
    }

    output.flush()
  }

  def read(input: InputStream, dec: SymmetricParams): Tree = {
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
        (bs.readString(), readSymmetricParams(bs))
      }
      entryNamesAndKey
    }

    val entries = entryIdsAndModes.zip(entryNamesAndKey)
      .map(e => TreeEntry(e._1._1, e._1._2, e._2._1, e._2._2))

    return Tree(ObjectId(), issuer, entries)
  }
}

private[objects] object CommitSerializer {
  def write(output: OutputStream, commit: Commit, enc: SymmetricParams): Unit = {
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
        p => writeSymmetricParams(bs, p.key)
      }
      writeSymmetricParams(bs, commit.tree.key)
    }

    output.flush()
  }

  def read(input: InputStream, dec: SymmetricParams): Commit = {
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
        readSymmetricParams(bs)
      }
      val treeKey = readSymmetricParams(bs)
      (parentIds, treeKey)
    }

    val parents = parentIds.zip(parentKeys)
      .map(p => CommitParent(p._1, p._2))
    val tree = TreeEntry(treeId, DirectoryTreeEntryMode, "", treeKey)

    return Commit(ObjectId(), issuer, parents, tree)
  }
}
