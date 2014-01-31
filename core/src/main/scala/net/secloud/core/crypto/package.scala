package net.secloud.core

import java.io._
import net.secloud.core.utils.BinaryReaderWriter._
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
  val asymmetricAlgorithmMapInverse = inverseMap(symmetricAlgorithmMap)

  def writeSymmetricAlgorithm(output: OutputStream, key: SymmetricAlgorithmInstance): Unit = {
    val algorithm = key.algorithm
    output.writeInt8(symmetricAlgorithmMap(algorithm))
    algorithm.save(output, key)
  }

  def readSymmetricAlgorithm(input: InputStream): SymmetricAlgorithmInstance = {
    val algorithm = symmetricAlgorithmMapInverse(input.readInt8())
    algorithm.load(input)
  }

  private def inverseMap[A, B](map: Map[A, B]): Map[B, A] =  map.map(entry => (entry._2, entry._1))
}
