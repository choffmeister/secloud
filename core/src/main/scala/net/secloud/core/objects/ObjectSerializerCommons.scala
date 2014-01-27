package net.secloud.core.objects

import java.io.OutputStream
import java.io.InputStream
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import net.secloud.core.utils._
import net.secloud.core.utils.BlockStream._
import net.secloud.core.utils.BinaryReaderWriter._
import net.secloud.core.security._
import com.jcraft.jzlib.{GZIPInputStream, GZIPOutputStream}

class ObjectSerializationException(msg: String) extends Exception(msg)

private[objects] object ObjectSerializerCommons {
  import ObjectSerializerConstants._

  def readHeader(stream: InputStream): ObjectType = {
    val magicBytes = stream.readInt32()
    assert("Invalid magic bytes", magicBytes == MagicBytes)
    objectTypeMapInverse(stream.readInt8())
  }

  def writeHeader(stream: OutputStream, objectType: ObjectType) {
    stream.writeInt32(MagicBytes)
    stream.writeInt8(objectTypeMap(objectType))
  }

  def readIssuerIdentityBlock(stream: InputStream): Issuer = {
    readBlock(stream, IssuerIdentityBlockType) { bs =>
      val id = bs.readBinary()
      val name = bs.readString()
      Issuer(id, name)
    }
  }

  def writeIssuerIdentityBlock(stream: OutputStream, issuer: Issuer) {
    writeBlock(stream, IssuerIdentityBlockType) { bs =>
      bs.writeBinary(issuer.id)
      bs.writeString(issuer.name)
    }
  }

  def readIssuerSignatureBlock(stream: InputStream): Seq[Byte] = {
    readBlock(stream, IssuerSignatureBlockType) { bs =>
      bs.readBinary().toSeq
    }
  }

  def writeIssuerSignatureBlock(stream: OutputStream, signature: Seq[Byte]) {
    writeBlock(stream, IssuerSignatureBlockType) { bs =>
      bs.writeBinary(signature)
    }
  }

  def readPublicBlock[T](stream: InputStream)(inner: InputStream => T): T = {
    readBlock(stream, PublicBlockType) { bs =>
      inner(bs)
    }
  }

  def writePublicBlock(stream: OutputStream)(inner: OutputStream => Any): Unit = {
    writeBlock(stream, PublicBlockType) { bs =>
      inner(bs)
    }
  }

  def readPrivateBlock[T](stream: InputStream, decrypt: SymmetricParams)(inner: InputStream => T): T = {
    readBlock(stream, PrivateBlockType) { bs =>
      val ds = decrypt.algorithm.wrapStream(bs, decrypt)
      inner(ds)
    }
  }

  def writePrivateBlock(stream: OutputStream, encrypt: SymmetricParams)(inner: OutputStream => Any): Unit = {
    writeBlock(stream, PrivateBlockType) { bs =>
      val ds = encrypt.algorithm.wrapStream(bs, encrypt)
      inner(ds)
      ds.flush()
      ds.close()
    }
  }

  def readBlock[T](stream: InputStream, expectedBlockType: BlockType)(inner: InputStream => T): T = {
    val actualBlockType = blockTypeMapInverse(stream.readInt8())
    assert(s"Expected block of type '${expectedBlockType.getClass.getSimpleName}'", expectedBlockType == actualBlockType)

    val blockStream = new BlockInputStream(stream, ownsInner = false)
    val result = inner(blockStream)
    blockStream.close()
    result
  }

  def writeBlock(stream: OutputStream, blockType: BlockType)(inner: OutputStream => Any) {
    stream.writeInt8(blockTypeMap(blockType))

    val blockStream = new BlockOutputStream(stream, ownsInner = false)
    inner(blockStream)
    blockStream.close()
  }

  def readCompressed[T](stream: InputStream)(inner: InputStream => T): T = {
    val gzip = new GZIPInputStream(stream)
    try {
      inner(gzip)
    } finally {
      gzip.close()
    }
  }

  def writeCompressed(stream: OutputStream)(inner: OutputStream => Any) {
    val gzip = new GZIPOutputStream(stream)
    try {
      inner(gzip)
      gzip.flush()
    } finally {
      gzip.close()
    }
  }

  def assert(errorMessage: String, cond: Boolean): Unit = assert(errorMessage)(cond == true)

  def assert(errorMessage: String)(cond: => Boolean): Unit = {
    if (cond == false) {
      throw new ObjectSerializationException(errorMessage)
    }
  }
}
