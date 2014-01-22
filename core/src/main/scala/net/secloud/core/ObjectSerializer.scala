package net.secloud.core

import java.io.OutputStream
import java.io.InputStream
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import net.secloud.core.utils._
import net.secloud.core.utils.BlockStream._
import net.secloud.core.utils.BinaryReaderWriter._

object ObjectSerializerConstants {
  val MagicBytes = 0x12345678

  sealed abstract class BlockType
  case object IssuerIdentityBlockType extends BlockType
  case object IssuerSignatureBlockType extends BlockType
  case object PublicBlockType extends BlockType
  case object PrivateBlockType extends BlockType

  val blockTypeMap = Map[BlockType, Byte](
    IssuerIdentityBlockType -> 0x00,
    IssuerSignatureBlockType -> 0x01,
    PublicBlockType -> 0x02,
    PrivateBlockType -> 0x03
  )
  val blockTypeMapInverse = blockTypeMap.map(entry => (entry._2, entry._1))

  sealed abstract class ObjectType
  case object BlobObjectType extends ObjectType
  case object TreeObjectType extends ObjectType
  case object CommitObjectType extends ObjectType

  val objectTypeMap = Map[ObjectType, Byte](
    BlobObjectType -> 0x00,
    TreeObjectType -> 0x01,
    CommitObjectType -> 0x02
  )
  val objectTypeMapInverse = objectTypeMap.map(entry => (entry._2, entry._1))
}

class ObjectSerializationException(msg: String) extends Exception(msg)

object ObjectSerializer {
  import ObjectSerializerConstants._
  import security.CryptographicAlgorithms._

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

  def readPrivateBlock[T](stream: InputStream, decrypt: SymmetricEncryptionParameters)(inner: InputStream => T): T = {
    readBlock(stream, PrivateBlockType) { bs =>
      val ds = decrypt.algorithm.wrapStream(bs, decrypt)
      inner(ds)
    }
  }

  def writePrivateBlock(stream: OutputStream, encrypt: SymmetricEncryptionParameters)(inner: OutputStream => Any): Unit = {
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

  def assert(errorMessage: String, cond: Boolean): Unit = assert(errorMessage)(cond == true)

  def assert(errorMessage: String)(cond: => Boolean): Unit = {
    if (cond == false) {
      throw new ObjectSerializationException(errorMessage)
    }
  }
}
