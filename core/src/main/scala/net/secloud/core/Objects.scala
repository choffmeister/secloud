package net.secloud.core

import java.io.InputStream
import java.io.OutputStream
import net.secloud.core.ObjectSerializer._
import net.secloud.core.security.CryptographicAlgorithms._
import net.secloud.core.utils.RichStream._
import net.secloud.core.ObjectSerializerConstants._
import com.jcraft.jzlib.{GZIPInputStream, GZIPOutputStream}

case class Issuer(id: Seq[Byte], name: String)

abstract class BaseObject {
  val id: Option[ObjectId]
  val issuer: Issuer
  val objectType: ObjectType
}

case class Blob(
  id: Option[ObjectId],
  issuer: Issuer
) extends BaseObject {
  val objectType = BlobObjectType
}

case class Tree(
  id: Option[ObjectId],
  issuer: Issuer
) extends BaseObject {
  val objectType = TreeObjectType
}

case class Commit(
  id: Option[ObjectId],
  issuer: Issuer
) extends BaseObject {
  val objectType = CommitObjectType
}

object Blob {
  def write(output: OutputStream, blob: Blob, content: InputStream, enc: SymmetricEncryptionParameters): Unit = {
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
  }

  def read(input: InputStream, content: OutputStream, dec: SymmetricEncryptionParameters): Blob = {
    val ds = `SHA-2-256`.wrapStream(input)
    val objectType = readHeader(ds)
    assert("Expected blob", objectType == BlobObjectType)
    val issuer = readIssuerIdentityBlock(ds)
    val publicBlock = readPublicBlock(ds) { bs =>
    }
    val privateBlock = readPrivateBlock(ds, dec) { bs =>
      val gzip = new GZIPInputStream(bs)
      gzip.pipeTo(content)
      gzip.close()
    }
    val digest = ds.getMessageDigest.digest.toSeq
    val signature = readIssuerSignatureBlock(input)
    // TODO: validate signature with hash
    assert("Signature invalid", signature == digest)
    return Blob(Some(ObjectId(digest)), issuer)
  }
}

object Tree {
  def write(output: OutputStream, tree: Tree, enc: SymmetricEncryptionParameters): Unit = {
    val ds = `SHA-2-256`.wrapStream(output)
    writeHeader(ds, tree.objectType)
    writeIssuerIdentityBlock(ds, tree.issuer)
    writePublicBlock(ds) { bs =>
    }
    writePrivateBlock(ds, enc) { bs =>
    }
    ds.flush()
    val digest = ds.getMessageDigest().digest.toSeq
    // TODO: sign hash
    writeIssuerSignatureBlock(output, digest)
  }

  def read(input: InputStream, dec: SymmetricEncryptionParameters): Tree = {
    val ds = `SHA-2-256`.wrapStream(input)
    val objectType = readHeader(ds)
    assert("Expected tree", objectType == TreeObjectType)
    val issuer = readIssuerIdentityBlock(ds)
    val publicBlock = readPublicBlock(ds) { bs =>
    }
    val privateBlock = readPrivateBlock(ds, dec) { bs =>
    }
    val digest = ds.getMessageDigest().digest.toSeq
    val signature = readIssuerSignatureBlock(input)
    // TODO: validate signature with hash
    assert("Signature invalid", signature == digest)
    return Tree(Some(ObjectId(digest)), issuer)
  }
}

object Commit {
  def write(output: OutputStream, commit: Commit, enc: SymmetricEncryptionParameters): Unit = {
    val ds = `SHA-2-256`.wrapStream(output)
    writeHeader(ds, commit.objectType)
    writeIssuerIdentityBlock(ds, commit.issuer)
    writePublicBlock(ds) { bs =>
    }
    writePrivateBlock(ds, enc) { bs =>
    }
    ds.flush()
    val digest = ds.getMessageDigest().digest.toSeq
    // TODO: sign hash
    writeIssuerSignatureBlock(output, digest)
  }

  def read(input: InputStream, dec: SymmetricEncryptionParameters): Commit = {
    val ds = `SHA-2-256`.wrapStream(input)
    val objectType = readHeader(ds)
    assert("Expected commit", objectType == CommitObjectType)
    val issuer = readIssuerIdentityBlock(ds)
    val publicBlock = readPublicBlock(ds) { bs =>
    }
    val privateBlock = readPrivateBlock(ds, dec) { bs =>
    }
    val digest = ds.getMessageDigest().digest.toSeq
    val signature = readIssuerSignatureBlock(input)
    // TODO: validate signature with hash
    assert("Signature invalid", signature == digest)
    return Commit(Some(ObjectId(digest)), issuer)
  }
}
