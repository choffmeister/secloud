package net.secloud.core

import java.io._
import net.secloud.core.utils._
import net.secloud.core.crypto.SymmetricAlgorithmInstance

package object crypto {
  val symmetricAlgorithmMap = Map[SymmetricAlgorithm, Byte](
    NullEncryption -> 0x00,
    AES -> 0x01
  )
  val symmetricAlgorithmMapInverse = inverseMap(symmetricAlgorithmMap)

  val asymmetricAlgorithmMap = Map[AsymmetricAlgorithm, Byte](
    RSA -> 0x00
  )
  val asymmetricAlgorithmMapInverse = inverseMap(asymmetricAlgorithmMap)

  val hashAlgorithmMap = Map[HashAlgorithm, Byte](
    SHA1 -> 0x00,
    SHA256 -> 0x01,
    SHA384 -> 0x02,
    SHA512 -> 0x03
  )
  val hashAlgorithmMapInverse = inverseMap(hashAlgorithmMap)

  def writeSymmetricAlgorithm(output: OutputStream, key: SymmetricAlgorithmInstance): Unit = {
    val algorithm = key.algorithm
    output.writeInt8(symmetricAlgorithmMap(algorithm))
    algorithm.save(output, key)
  }

  def readSymmetricAlgorithm(input: InputStream): SymmetricAlgorithmInstance = {
    val algorithm = symmetricAlgorithmMapInverse(input.readInt8())
    algorithm.load(input)
  }

  def writeAsymmetricAlgorithm(output: OutputStream, key: AsymmetricAlgorithmInstance, includePrivate: Boolean): Unit = {
    val algorithm = key.algorithm
    output.writeInt8(asymmetricAlgorithmMap(algorithm))
    algorithm.save(output, key, includePrivate)
  }

  def readAsymmetricAlgorithm(input: InputStream): AsymmetricAlgorithmInstance = {
    val algorithm = asymmetricAlgorithmMapInverse(input.readInt8())
    algorithm.load(input)
  }

  def writeHashAlgorithm(output: OutputStream, hash: HashAlgorithmInstance): Unit = {
    val algorithm = hash.algorithm
    output.writeInt8(hashAlgorithmMap(algorithm))
  }

  def readHashAlgorithm(input: InputStream): HashAlgorithmInstance = {
    val algorithm = hashAlgorithmMapInverse(input.readInt8())
    algorithm.create()
  }

  private def inverseMap[A, B](map: Map[A, B]): Map[B, A] =  map.map(entry => (entry._2, entry._1))
}
