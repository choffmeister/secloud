package de.choffmeister.secloud.core

import java.io.InputStream
import java.security.MessageDigest
import java.security.DigestInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.io.OutputStream
import java.security.DigestOutputStream
import de.choffmeister.secloud.core.security.CryptographicAlgorithms._

class ObjectInputStream(stream: InputStream, val hashAlgorithm: HashAlgorithm) extends InputStream {
  private val digest = MessageDigest.getInstance(hashAlgorithm.algorithmName)
  private val digestStream = new DigestInputStream(stream, digest)
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
    val byte = digestStream.read().toByte

    if (sizeIssuerBlock.isEmpty) {
      val sizePosition = 5
      if (sizePosition <= position && position < sizePosition + 8) {
        bufRaw((position - sizePosition).toInt) = byte
        if (position == sizePosition + 7) {
          sizeIssuerBlock = Some(buf.getLong(0))
        }
      }
    } else if (sizePublicBlock.isEmpty) {
      val sizePosition = 5 + 8 + sizeIssuerBlock.get
      if (sizePosition <= position && position < sizePosition + 8) {
        bufRaw((position - sizePosition).toInt) = byte
        if (position == sizePosition + 7) {
          sizePublicBlock = Some(buf.getLong(0))
        }
      }
    } else if (sizePrivateBlock.isEmpty) {
      val sizePosition = 5 + 8 + sizeIssuerBlock.get + 8 + sizePublicBlock.get
      if (sizePosition <= position && position < sizePosition + 8) {
        bufRaw((position - sizePosition).toInt) = byte
        if (position == sizePosition + 7) {
          sizePrivateBlock = Some(buf.getLong(0))
        }
      }
    } else {
      val signaturePosition = 5 + 8 + sizeIssuerBlock.get + 8 + sizePublicBlock.get + 8 + sizePrivateBlock.get
      val p1 = sizeIssuerBlock.get
      val p2 = sizePublicBlock.get
      val p3 = sizePrivateBlock.get
      val p = position
      if (position == signaturePosition - 1) {
        digestStream.on(false)
        hashIntern = Some(digest.digest())
      }
    }

    position = position + 1
    byte
  }
}

class ObjectOutputStream(stream: OutputStream, val hashAlgorithm: HashAlgorithm) extends OutputStream {
  private val digest = MessageDigest.getInstance(hashAlgorithm.algorithmName)
  private val digestStream = new DigestOutputStream(stream, digest)
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
      val sizePosition = 5
      if (sizePosition <= position && position < sizePosition + 8) {
        bufRaw((position - sizePosition).toInt) = byte
        if (position == sizePosition + 7) {
          sizeIssuerBlock = Some(buf.getLong(0))
        }
      }
    } else if (sizePublicBlock.isEmpty) {
      val sizePosition = 5 + 8 + sizeIssuerBlock.get
      if (sizePosition <= position && position < sizePosition + 8) {
        bufRaw((position - sizePosition).toInt) = byte
        if (position == sizePosition + 7) {
          sizePublicBlock = Some(buf.getLong(0))
        }
      }
    } else if (sizePrivateBlock.isEmpty) {
      val sizePosition = 5 + 8 + sizeIssuerBlock.get + 8 + sizePublicBlock.get
      if (sizePosition <= position && position < sizePosition + 8) {
        bufRaw((position - sizePosition).toInt) = byte
        if (position == sizePosition + 7) {
          sizePrivateBlock = Some(buf.getLong(0))
        }
      }
    } else {
      val signaturePosition = 5 + 8 + sizeIssuerBlock.get + 8 + sizePublicBlock.get + 8 + sizePrivateBlock.get
      if (position == signaturePosition - 1) {
        digestStream.on(false)
        hashIntern = Some(digest.digest())
      }
    }

    position = position + 1
  }
}

object ObjectStream {

}