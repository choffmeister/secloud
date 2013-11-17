package de.choffmeister.secloud.core

import java.io.InputStream
import java.io.OutputStream
import de.choffmeister.secloud.core.ObjectSerializer._
import de.choffmeister.secloud.core.security.CryptographicAlgorithms._
import de.choffmeister.secloud.core.utils.RichStream._
import de.choffmeister.secloud.core.ObjectSerializerConstants._

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
    val os = new ObjectHashOutputStream(output, `SHA-2-256`)
    writeHeader(os, blob.objectType)
    writeIssuerIdentityBlock(os, blob.issuer)
    writePublicBlock(os) { bs =>
    }
    // TODO: get content size in advance to avoid unnecessary caching
    writePrivateBlock(os, enc) { bs =>
      content.pipeTo(bs)
    }
    // TODO: sign hash
    writeIssuerSignatureBlock(os, os.hash.get)
  }

  def read(input: InputStream, content: OutputStream, dec: SymmetricEncryptionParameters): Blob = {
    val os = new ObjectHashInputStream(input, `SHA-2-256`)
    val objectType = readHeader(os)
    assert("Expected blob", objectType == BlobObjectType)
    val issuer = readIssuerIdentityBlock(os)
    val publicBlock = readPublicBlock(os) { bs =>
    }
    val privateBlock = readPrivateBlock(os, dec) { bs =>
      bs.pipeTo(content)
    }
    val signature = readIssuerSignatureBlock(os)
    // TODO: validate signature with hash
    assert("Signature invalid", signature == os.hash.get)
    return Blob(Some(ObjectId(os.hash.get)), issuer)
  }
}

object Tree {
  def write(output: OutputStream, tree: Tree, enc: SymmetricEncryptionParameters): Unit = {
    val os = new ObjectHashOutputStream(output, `SHA-2-256`)
    writeHeader(os, tree.objectType)
    writeIssuerIdentityBlock(os, tree.issuer)
    writePublicBlock(os) { bs =>
    }
    writePrivateBlock(os, enc) { bs =>
    }
    // TODO: sign hash
    writeIssuerSignatureBlock(os, os.hash.get)
  }

  def read(input: InputStream, dec: SymmetricEncryptionParameters): Tree = {
    val os = new ObjectHashInputStream(input, `SHA-2-256`)
    val objectType = readHeader(os)
    assert("Expected tree", objectType == TreeObjectType)
    val issuer = readIssuerIdentityBlock(os)
    val publicBlock = readPublicBlock(os) { bs =>
    }
    val privateBlock = readPrivateBlock(os, dec) { bs =>
    }
    val signature = readIssuerSignatureBlock(os)
    // TODO: validate signature with hash
    assert("Signature invalid", signature == os.hash.get)
    return Tree(Some(ObjectId(os.hash.get)), issuer)
  }
}

object Commit {
  def write(output: OutputStream, commit: Commit, enc: SymmetricEncryptionParameters): Unit = {
    val os = new ObjectHashOutputStream(output, `SHA-2-256`)
    writeHeader(os, commit.objectType)
    writeIssuerIdentityBlock(os, commit.issuer)
    writePublicBlock(os) { bs =>
    }
    writePrivateBlock(os, enc) { bs =>
    }
    // TODO: sign hash
    writeIssuerSignatureBlock(os, os.hash.get)
  }

  def read(input: InputStream, dec: SymmetricEncryptionParameters): Commit = {
    val os = new ObjectHashInputStream(input, `SHA-2-256`)
    val objectType = readHeader(os)
    assert("Expected commit", objectType == CommitObjectType)
    val issuer = readIssuerIdentityBlock(os)
    val publicBlock = readPublicBlock(os) { bs =>
    }
    val privateBlock = readPrivateBlock(os, dec) { bs =>
    }
    val signature = readIssuerSignatureBlock(os)
    // TODO: validate signature with hash
    assert("Signature invalid", signature == os.hash.get)
    return Commit(Some(ObjectId(os.hash.get)), issuer)
  }
}