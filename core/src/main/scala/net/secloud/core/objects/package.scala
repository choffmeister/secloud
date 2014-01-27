package net.secloud.core

import java.io.{InputStream, OutputStream}
import net.secloud.core.security._

package object objects {
  def writeBlob(output: OutputStream, blob: Blob): Unit =
    BlobSerializer.write(output, blob)
  def readBlob(input: InputStream): Blob =
    BlobSerializer.read(input)
  def writeBlobContent(output: OutputStream, dec: SymmetricParams)(inner: OutputStream => Any): Unit =
    BlobSerializer.writeContent(output, dec)(inner)
  def readBlobContent[T](input: InputStream, enc: SymmetricParams)(inner: InputStream => T): T =
    BlobSerializer.readContent(input, enc)(inner)

  def writeTree(output: OutputStream, tree: Tree, dec: SymmetricParams): Unit =
    TreeSerializer.write(output, tree, dec)
  def readTree(input: InputStream, enc: SymmetricParams): Tree =
    TreeSerializer.read(input, enc)

  def writeCommit(output: OutputStream, commit: Commit, dec: SymmetricParams): Unit =
    CommitSerializer.write(output, commit, dec)
  def readCommit(input: InputStream, enc: SymmetricParams): Commit =
    CommitSerializer.read(input, enc)

  def signObject(output: OutputStream)(inner: OutputStream => Any): ObjectId = {
    val hashStream = `SHA-1`.wrapStream(output)
    inner(hashStream)
    hashStream.flush()

    val digest = hashStream.getMessageDigest.digest.toSeq
    // TODO: sign with a private key
    val signature = digest
    ObjectSerializerCommons.writeIssuerSignatureBlock(output, digest)
    output.flush()

    return ObjectId(digest)
  }

  def validateObject[T](input: InputStream)(inner: InputStream => T): T = {
    val hashStream = `SHA-1`.wrapStream(input)
    val result = inner(hashStream)

    val digest = hashStream.getMessageDigest.digest.toSeq
    val signature = ObjectSerializerCommons.readIssuerSignatureBlock(input)
    // TODO: validate signature against a public key
    if (digest != signature) throw new Exception("Invalid signature")

    return result
  }
}
