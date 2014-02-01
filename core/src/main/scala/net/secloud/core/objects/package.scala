package net.secloud.core

import java.io.{InputStream, OutputStream}
import net.secloud.core.crypto._

package object objects {
  def writeBlob(output: OutputStream, blob: Blob): Unit =
    BlobSerializer.write(output, blob)
  def readBlob(input: InputStream): Blob =
    BlobSerializer.read(input)
  def writeBlobContent(output: OutputStream, key: SymmetricAlgorithmInstance)(inner: OutputStream => Any): Unit =
    BlobSerializer.writeContent(output, key)(inner)
  def readBlobContent[T](input: InputStream, key: SymmetricAlgorithmInstance)(inner: InputStream => T): T =
    BlobSerializer.readContent(input, key)(inner)

  def writeTree(output: OutputStream, tree: Tree, key: SymmetricAlgorithmInstance): Unit =
    TreeSerializer.write(output, tree, key)
  def readTree(input: InputStream, enc: SymmetricAlgorithmInstance): Tree =
    TreeSerializer.read(input, enc)

  def writeCommit(output: OutputStream, commit: Commit, key: SymmetricAlgorithmInstance): Unit =
    CommitSerializer.write(output, commit, key)
  def readCommit(input: InputStream, enc: SymmetricAlgorithmInstance): Commit =
    CommitSerializer.read(input, enc)

  def signObject(output: OutputStream, privateKey: AsymmetricAlgorithmInstance)(inner: OutputStream => Any): ObjectId =
    ObjectSerializerCommons.signObject(output, privateKey)(inner)
  def validateObject[T](input: InputStream, publicKeys: Map[Seq[Byte], AsymmetricAlgorithmInstance])(inner: InputStream => T): T =
    ObjectSerializerCommons.validateObject(input, publicKeys)(inner)
}
