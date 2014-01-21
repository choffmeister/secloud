package net.secloud.core

import java.io.InputStream
import java.security.MessageDigest
import java.security.DigestInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.io.OutputStream
import java.security.DigestOutputStream
import net.secloud.core.security.CryptographicAlgorithms._

class ObjectHashInputStream(val stream: InputStream, val hashAlgorithm: HashAlgorithm) extends InputStream {
  private val digestStream = hashAlgorithm.wrapStream(stream)
  private val bufRaw = new Array[Byte](8)
  private val buf = ByteBuffer.wrap(bufRaw)
  buf.order(ByteOrder.BIG_ENDIAN)

  private var position: Long = 0
  private var sizeIssuerBlock: Option[Long] = None
  private var sizePublicBlock: Option[Long] = None
  private var sizePrivateBlock: Option[Long] = None
  private var hashIntern: Option[Seq[Byte]] = None

  def hash: Option[Seq[Byte]] = hashIntern

  override def read(): Int = {
    val byte = digestStream.read()

    if (sizeIssuerBlock.isEmpty) {
      val sizePosition = 5 + 1
      if (sizePosition <= position && position < sizePosition + 8) {
        bufRaw((position - sizePosition).toInt) = byte.toByte
        if (position == sizePosition + 7) {
          sizeIssuerBlock = Some(buf.getLong(0))
        }
      }
    } else if (sizePublicBlock.isEmpty) {
      val sizePosition = 5 + 1 + 8 + sizeIssuerBlock.get + 1
      if (sizePosition <= position && position < sizePosition + 8) {
        bufRaw((position - sizePosition).toInt) = byte.toByte
        if (position == sizePosition + 7) {
          sizePublicBlock = Some(buf.getLong(0))
        }
      }
    } else if (sizePrivateBlock.isEmpty) {
      val sizePosition = 5 + 1 + 8 + sizeIssuerBlock.get + 1 + 8 + sizePublicBlock.get + 1
      if (sizePosition <= position && position < sizePosition + 8) {
        bufRaw((position - sizePosition).toInt) = byte.toByte
        if (position == sizePosition + 7) {
          sizePrivateBlock = Some(buf.getLong(0))

          if (sizePrivateBlock == Some(0)) {
            digestStream.on(false)
            hashIntern = Some(digestStream.getMessageDigest.digest())
          }
        }
      }
    } else {
      val signaturePosition = 5 + 1 + 8 + sizeIssuerBlock.get + 1 + 8 + sizePublicBlock.get + 1 + 8 + sizePrivateBlock.get
      if (position == signaturePosition - 1) {
        digestStream.on(false)
        hashIntern = Some(digestStream.getMessageDigest.digest())
      }
    }

    position = position + 1
    byte
  }
}

class ObjectHashOutputStream(val stream: OutputStream, val hashAlgorithm: HashAlgorithm) extends OutputStream {
  private val digestStream = hashAlgorithm.wrapStream(stream)
  private val bufRaw = new Array[Byte](8)
  private val buf = ByteBuffer.wrap(bufRaw)
  buf.order(ByteOrder.BIG_ENDIAN)

  private var position: Long = 0
  private var sizeIssuerBlock: Option[Long] = None
  private var sizePublicBlock: Option[Long] = None
  private var sizePrivateBlock: Option[Long] = None
  private var hashIntern: Option[Seq[Byte]] = None

  def hash: Option[Seq[Byte]] = hashIntern

  override def write(b: Int): Unit = {
    digestStream.write(b)
    val byte = b.toByte

    if (sizeIssuerBlock.isEmpty) {
      val sizePosition = 5 + 1
      if (sizePosition <= position && position < sizePosition + 8) {
        bufRaw((position - sizePosition).toInt) = byte
        if (position == sizePosition + 7) {
          sizeIssuerBlock = Some(buf.getLong(0))
        }
      }
    } else if (sizePublicBlock.isEmpty) {
      val sizePosition = 5 + 1 + 8 + sizeIssuerBlock.get + 1
      if (sizePosition <= position && position < sizePosition + 8) {
        bufRaw((position - sizePosition).toInt) = byte
        if (position == sizePosition + 7) {
          sizePublicBlock = Some(buf.getLong(0))
        }
      }
    } else if (sizePrivateBlock.isEmpty) {
      val sizePosition = 5 + 1 + 8 + sizeIssuerBlock.get + 1 + 8 + sizePublicBlock.get + 1
      if (sizePosition <= position && position < sizePosition + 8) {
        bufRaw((position - sizePosition).toInt) = byte
        if (position == sizePosition + 7) {
          sizePrivateBlock = Some(buf.getLong(0))

          if (sizePrivateBlock == Some(0)) {
            digestStream.flush()
            digestStream.on(false)
            hashIntern = Some(digestStream.getMessageDigest.digest())
          }
        }
      }
    } else {
      val signaturePosition = 5 + 1 + 8 + sizeIssuerBlock.get + 1 + 8 + sizePublicBlock.get + 1 + 8 + sizePrivateBlock.get
      if (position == signaturePosition - 1) {
        digestStream.flush()
        digestStream.on(false)
        hashIntern = Some(digestStream.getMessageDigest.digest())
      }
    }

    position = position + 1
  }
}

object ObjectHashStream {

}
