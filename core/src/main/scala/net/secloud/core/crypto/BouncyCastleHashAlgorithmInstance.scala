package net.secloud.core.crypto

import java.io._
import org.bouncycastle.crypto.Digest
import org.bouncycastle.crypto.io.{DigestInputStream, DigestOutputStream}

abstract class BouncyCastleHashAlgorithmInstance(protected val digest: Digest) extends HashAlgorithmInstance {
  def hash(output: OutputStream)(inner: OutputStream => Any): Array[Byte] = {
    val hs = new PassThroughDigestOutputStream(output, digest)
    inner(hs)
    hs.flush()
    val hash = hs.getDigest
    hash
  }

  def hash[T](input: InputStream)(inner: InputStream => T): (Array[Byte], T) = {
    val hs = new DigestInputStream(input, digest)
    val result = inner(hs)
    val hash = new Array[Byte](digest.getDigestSize())
    hs.getDigest.doFinal(hash, 0)
    (hash, result)
  }

  /**
   * This class is needed, since org.bouncycastle.crypto.io.DigestOutputStream
   * does not allow to passthrough the written bytes to an underlying stream.
   */
  private[BouncyCastleHashAlgorithmInstance] class PassThroughDigestOutputStream(val output: OutputStream, digest: Digest) extends DigestOutputStream(digest)
  {
    override def write(b: Int) {
      super.write(b)
      output.write(b)
    }

    override def write(b: Array[Byte], off: Int, len: Int) {
      super.write(b, off, len)
      output.write(b, off, len)
    }

    override def flush() = {
      super.flush()
      output.flush()
    }
  }
}
