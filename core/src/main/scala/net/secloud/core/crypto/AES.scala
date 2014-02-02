package net.secloud.core.crypto

import java.io._
import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.modes.CBCBlockCipher
import org.bouncycastle.crypto.paddings.{PaddedBufferedBlockCipher, PKCS7Padding}
import org.bouncycastle.crypto.params.{KeyParameter, ParametersWithIV}
import net.secloud.core.utils._

class AES(protected override val params: ParametersWithIV) extends BouncyCastleSymmetricAlgorithmInstance(params) {
  val algorithm = AES
  val blockSize = 16
  val keySize = params.getParameters.asInstanceOf[KeyParameter].getKey.length
  val name = s"AES-${keySize * 8}"

  protected def createCipher() = {
    val padding = new PKCS7Padding()
    new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()), padding)
  }
}

object AES extends SymmetricAlgorithm {
  def generate(keySize: Int): SymmetricAlgorithmInstance = {
    val key = RandomGenerator.nextBytes(keySize)
    val iv = RandomGenerator.nextBytes(16)

    create(key, iv)
  }

  def save(output: OutputStream, instance: SymmetricAlgorithmInstance): Unit = {
    val aes = instance.asInstanceOf[AES]
    val key = aes.params.getParameters.asInstanceOf[KeyParameter].getKey
    val iv = aes.params.getIV

    output.writeBinary(key)
    output.writeBinary(iv)
  }

  def load(input: InputStream): SymmetricAlgorithmInstance = {
    val key = input.readBinary()
    val iv = input.readBinary()

    create(key, iv)
  }

  private def create(key: Array[Byte], iv: Array[Byte]): AES = new AES(new ParametersWithIV(new KeyParameter(key), iv))
}
