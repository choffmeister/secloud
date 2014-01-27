package net.secloud.core.security

import java.io.{InputStream, OutputStream}
import net.secloud.core.utils.BinaryReaderWriter._

private[security] object CryptographicAlgorithmSerializerConstants {
  val symmetricAlgorithmMap = Map[SymmetricAlgorithm, Byte](
    NullEncryption -> 0x00,
    `AES-128` -> 0x01,
    `AES-192` -> 0x02,
    `AES-256` -> 0x03
  )
  val symmetricAlgorithmMapInverse = symmetricAlgorithmMap.map(entry => (entry._2, entry._1))

  val hashAlgorithmMap = Map[HashAlgorithm, Byte](
    `SHA-1`-> 0x00,
    `SHA-2-256` -> 0x01,
    `SHA-2-384` -> 0x02,
    `SHA-2-512` -> 0x03
  )
  val hashAlgorithmMapInverse = hashAlgorithmMap.map(entry => (entry._2, entry._1))
}

private[security] object CryptographicAlgorithmSerializer {
  import CryptographicAlgorithmSerializerConstants._

  def writeSymmetricParams(stream: OutputStream, parameters: SymmetricParams) {
    stream.writeInt8(symmetricAlgorithmMap(parameters.algorithm))
    parameters.algorithm.writeParameters(stream, parameters)
  }

  def readSymmetricParams(stream: InputStream): SymmetricParams = {
    val algorithm = symmetricAlgorithmMapInverse(stream.readInt8())
    algorithm.readParameters(stream)
  }

  def writeHashAlgorithm(stream: OutputStream, hashAlgorithm: HashAlgorithm) {
    stream.writeInt8(hashAlgorithmMap(hashAlgorithm))
  }

  def readHashAlgorithm(stream: InputStream): HashAlgorithm = {
    hashAlgorithmMapInverse(stream.readInt8())
  }
}
