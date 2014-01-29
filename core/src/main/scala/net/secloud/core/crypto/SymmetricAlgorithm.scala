package net.secloud.core.crypto

import java.io.{InputStream, OutputStream}
import org.bouncycastle.crypto.engines.NullEngine
import org.bouncycastle.crypto.io.{CipherInputStream, CipherOutputStream}
import org.bouncycastle.crypto.{BufferedBlockCipher, CipherParameters}

trait SymmetricAlgorithm {
  val name: String
  val keySize: Int
  val blockSize: Int

  def encrypt(output: OutputStream)(inner: OutputStream => Any): Unit
  def decrypt[T](input: InputStream)(inner: InputStream => T): T
}

abstract class BouncyCastleSymmetricAlgorithm(protected val params: CipherParameters) extends SymmetricAlgorithm {
  def encrypt(output: OutputStream)(inner: OutputStream => Any): Unit = {
    val cipher = createCipher()
    cipher.reset()
    cipher.init(true, params)

    val stream = new CipherOutputStream(output, cipher)
    inner(stream)
    stream.close()
  }

  def decrypt[T](input: InputStream)(inner: InputStream => T): T = {
    val cipher = createCipher()
    cipher.reset()
    cipher.init(false, params)

    val stream = new CipherInputStream(input, cipher)
    val result = inner(stream)
    stream.close()

    result
  }

  protected def createCipher(): BufferedBlockCipher
}

object NullEncryption extends BouncyCastleSymmetricAlgorithm(null) {
  val blockSize = 1
  val keySize = 0
  val name = "NULL"

  protected def createCipher() = {
    new BufferedBlockCipher(new NullEngine())
  }
}
