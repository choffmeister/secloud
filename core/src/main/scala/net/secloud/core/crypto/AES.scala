package net.secloud.core.crypto

import java.io._
import org.bouncycastle.crypto.engines.AESEngine
import org.bouncycastle.crypto.modes.CBCBlockCipher
import org.bouncycastle.crypto.paddings.{PaddedBufferedBlockCipher, PKCS7Padding}
import org.bouncycastle.crypto.params.{KeyParameter, ParametersWithIV}
import net.secloud.core.utils.BinaryReaderWriter._

class AES(protected override val params: ParametersWithIV) extends BouncyCastleSymmetricAlgorithm(params) {
  val blockSize = 16
  val keySize = params.getIV.length
  val name = s"AES-${keySize * 8}"

  protected def createCipher() = {
    val padding = new PKCS7Padding()
    new PaddedBufferedBlockCipher(new CBCBlockCipher(new AESEngine()), padding)
  }
}

object AES {
  def generate(keySize: Int): AES = {
    val key = RandomGenerator.nextBytes(keySize)
    val iv = RandomGenerator.nextBytes(16)

    create(key, iv)
  }

  def create(key: Array[Byte], iv: Array[Byte]): AES = {
    new AES(new ParametersWithIV(new KeyParameter(key), iv))
  }

  def save(output: OutputStream, aes: AES): Unit = {
    val key = aes.params.getParameters.asInstanceOf[KeyParameter].getKey
    val iv = aes.params.getIV

    output.writeBinary(key)
    output.writeBinary(iv)
  }

  def load(input: InputStream): AES = {
    val key = input.readBinary()
    val iv = input.readBinary()

    create(key, iv)
  }
}
