package net.secloud.core.crypto

import java.io.{InputStream, OutputStream}
import org.bouncycastle.crypto.BufferedBlockCipher
import org.bouncycastle.crypto.engines.NullEngine
import net.secloud.core.utils.BinaryReaderWriter._

class NullEncryption extends BouncyCastleSymmetricAlgorithmInstance(null) {
  val algorithm = NullEncryption
  val blockSize = 1
  val keySize = 0
  val name = "NULL"

  protected def createCipher() = new BufferedBlockCipher(new NullEngine())
}

object NullEncryption extends SymmetricAlgorithm {
  def generate(keySize: Int): SymmetricAlgorithmInstance = {
    if (keySize == 0) new NullEncryption()
    else throw new Exception("Invalid key size")
  }

  def save(output: OutputStream, instance: SymmetricAlgorithmInstance): Unit = {
    val ne = instance.asInstanceOf[NullEncryption]
    output.writeBinary(Array.empty[Byte])
  }

  def load(input: InputStream): SymmetricAlgorithmInstance = {
    if (input.readBinary().length == 0) new NullEncryption()
    else throw new Exception("Invalid key size")
  }
}
