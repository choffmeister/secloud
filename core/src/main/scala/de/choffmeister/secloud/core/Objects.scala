package de.choffmeister.secloud.core

import java.io.OutputStream
import java.io.InputStream
import de.choffmeister.secloud.core.utils._
import de.choffmeister.secloud.core.utils.RichStream._
import de.choffmeister.secloud.core.utils.BinaryReaderWriter._
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream

case class IssuerInformation(id: Array[Byte], name: String, signature: Array[Byte])

abstract class BaseObject {
  val id: ObjectId
  val objectType: ObjectSerializerConstants.ObjectType
  val issuer: IssuerInformation
}

case class Blob(
  id: ObjectId,
  issuer: IssuerInformation
) extends BaseObject {
  val objectType = ObjectSerializerConstants.BlobObjectType
}

case class Tree(
  id: ObjectId,
  issuer: IssuerInformation
) extends BaseObject {
  val objectType = ObjectSerializerConstants.TreeObjectType
}

case class Commit(
  id: ObjectId,
  issuer: IssuerInformation
) extends BaseObject {
  val objectType = ObjectSerializerConstants.CommitObjectType
}

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

  def serialize(obj: BaseObject): Array[Byte] = {
    val ms = new ByteArrayOutputStream()
    serialize(obj, ms)
    ms.toByteArray()
  }

  def serialize(obj: BaseObject, stream: OutputStream): Unit = {
    writeHeader(stream, obj.objectType)

    // issuer identity block
    writeBlock(stream, IssuerIdentityBlockType) { bs =>
      bs.writeBinary(obj.issuer.id)
      bs.writeString(obj.issuer.name)
    }

    // public block
    writeBlock(stream, PublicBlockType) { bs =>
    }

    // private block
    writeBlock(stream, PrivateBlockType) { bs =>
    }

    // issuer signature block
    writeBlock(stream, IssuerSignatureBlockType) { bs =>
      bs.writeBinary(obj.issuer.signature)
    }

    stream.writeObjectId(obj.id)
  }

  def deserialize(buf: Array[Byte]): BaseObject = {
    val ms = new ByteArrayInputStream(buf)
    deserialize(ms)
  }

  def deserialize(stream: InputStream): BaseObject = {
    val objectType = readHeader(stream)
    var issuer = IssuerInformation(Array.empty, "", Array.empty)

    // issuer identity block
    readBlock(stream, IssuerIdentityBlockType) { bs =>
      val issuerId = bs.readBinary()
      val issuerName = bs.readString()
      issuer = issuer.copy(id = issuerId, name = issuerName)
    }

    // public block
    readBlock(stream, PublicBlockType) { bs =>
    }

    // private block
    readBlock(stream, PrivateBlockType) { bs =>
    }

    // issuer signature block
    readBlock(stream, IssuerSignatureBlockType) { bs =>
      val issuerSignature = bs.readBinary()
      issuer = issuer.copy(signature = issuerSignature)
    }

    val objectId = stream.readObjectId()
    Blob(objectId, issuer)
  }

  def readHeader(stream: InputStream): ObjectType = {
    val magicBytes = stream.readInt32()
    assert("Invalid magic bytes", magicBytes == MagicBytes)
    objectTypeMapInverse(stream.readInt8())
  }

  def writeHeader(stream: OutputStream, objectType: ObjectType) {
    stream.writeInt32(MagicBytes)
    stream.writeInt8(objectTypeMap(objectType))
  }

  def readBlock(stream: InputStream, expectedBlockType: BlockType)(inner: InputStream => Any) {
    stream.preSizedInner(stream.readInt64()) { is =>
      val actualBlockType = blockTypeMapInverse(is.readInt8())
      assert(s"Expected block of type '${expectedBlockType.getClass.getSimpleName}'", expectedBlockType == actualBlockType)
      inner(is)
    }
  }

  def writeBlock(stream: OutputStream, blockType: BlockType)(inner: OutputStream => Any) {
    stream.cached(cs => stream.writeInt64(cs.size)) { cs =>
      cs.writeInt8(blockTypeMap(blockType))
      inner(cs)
    }
  }

  def writeBlock(stream: OutputStream, innerSize: Long, blockType: BlockType)(inner: OutputStream => Any) {
    stream.writeInt64(innerSize + 1L)
    stream.writeInt8(blockTypeMap(blockType))
    stream.preSizedInner(innerSize)(inner)
  }

  def assert(errorMessage: String, cond: Boolean): Unit = assert(errorMessage)(cond == true)

  def assert(errorMessage: String)(cond: => Boolean): Unit = {
    if (cond == false) {
      throw new ObjectSerializationException(errorMessage)
    }
  }
}