package net.secloud.core

import java.io.{InputStream, OutputStream}

package object security {
  def writeSymmetricParams(stream: OutputStream, parameters: SymmetricParams): Unit =
    CryptographicAlgorithmSerializer.writeSymmetricParams(stream, parameters)

  def readSymmetricParams(stream: InputStream): SymmetricParams =
    CryptographicAlgorithmSerializer.readSymmetricParams(stream)

  def writeHashAlgorithm(stream: OutputStream, hashAlgorithm: HashAlgorithm): Unit =
    CryptographicAlgorithmSerializer.writeHashAlgorithm(stream, hashAlgorithm)

  def readHashAlgorithm(stream: InputStream): HashAlgorithm =
    CryptographicAlgorithmSerializer.readHashAlgorithm(stream)
}
