package net.secloud.core.crypto

import java.io.{InputStream, OutputStream}
import org.bouncycastle.crypto.io.{CipherInputStream, CipherOutputStream}
import org.bouncycastle.crypto.{BufferedBlockCipher, CipherParameters}

abstract class BouncyCastleSymmetricAlgorithmInstance(protected val params: CipherParameters) extends SymmetricAlgorithmInstance {
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
