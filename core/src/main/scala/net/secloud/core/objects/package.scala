package net.secloud.core

import java.io.{InputStream, OutputStream}
import net.secloud.core.security.CryptographicAlgorithms.SymmetricEncryptionParameters

package object objects {
  def writeBlob(output: OutputStream, blob: Blob, content: InputStream, dec: SymmetricEncryptionParameters): Blob =
    BlobSerializer.write(output, blob, content, dec)
  def readBlob(input: InputStream, content: OutputStream, enc: SymmetricEncryptionParameters): Blob =
    BlobSerializer.read(input, content, enc)

  def writeTree(output: OutputStream, tree: Tree, dec: SymmetricEncryptionParameters): Tree =
    TreeSerializer.write(output, tree, dec)
  def readTree(input: InputStream, enc: SymmetricEncryptionParameters): Tree =
    TreeSerializer.read(input, enc)

  def writeCommit(output: OutputStream, commit: Commit, dec: SymmetricEncryptionParameters): Commit =
    CommitSerializer.write(output, commit, dec)
  def readCommit(input: InputStream, enc: SymmetricEncryptionParameters): Commit =
    CommitSerializer.read(input, enc)
}
