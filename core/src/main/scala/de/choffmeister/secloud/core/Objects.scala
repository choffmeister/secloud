package de.choffmeister.secloud.core

import java.io.OutputStream
import java.io.InputStream
import de.choffmeister.secloud.core.utils._
import de.choffmeister.secloud.core.utils.RichStream._
import de.choffmeister.secloud.core.utils.BinaryReaderWriter._
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream

case class Issuer(id: Array[Byte], name: String)

object Issuer {
  def apply(): Issuer = Issuer(Array.empty[Byte], "")
}

abstract class BaseObject {
  val id: ObjectId
  val issuer: Issuer
  val objectType: ObjectSerializerConstants.ObjectType
}

case class Blob(
  id: ObjectId,
  issuer: Issuer
) extends BaseObject {
  val objectType = ObjectSerializerConstants.BlobObjectType
}

case class Tree(
  id: ObjectId,
  issuer: Issuer
) extends BaseObject {
  val objectType = ObjectSerializerConstants.TreeObjectType
}

case class Commit(
  id: ObjectId,
  issuer: Issuer
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
  import security.CryptographicAlgorithms._

  def serialize(obj: BaseObject, stream: OutputStream): Unit = {
    val hash = writeHashed(stream, `SHA-2-256`) { hs =>
      writeHeader(hs, obj.objectType)
      writeIssuerIdentityBlock(hs, obj.issuer)

      // public block
      writeBlock(hs, PublicBlockType) { bs =>
      }

      // private block
      writeBlock(hs, PrivateBlockType) { bs =>
      }
    }

    // issuer signature block
    writeBlock(stream, IssuerSignatureBlockType) { bs =>
      // TODO: sign hash
      bs.writeBinary(hash)
    }
  }

  def deserialize(id: ObjectId, stream: InputStream): BaseObject = {
    val hash = readHashed(stream, `SHA-2-256`) { hs =>
      val objectType = readHeader(stream)
      val issuer = readIssuerIdentityBlock(hs)

      // public block
      readBlock(stream, PublicBlockType) { bs =>
      }

      // private block
      readBlock(stream, PrivateBlockType) { bs =>
      }

      (objectType, issuer)
    }

    // issuer signature block
    readBlock(stream, IssuerSignatureBlockType) { bs =>
      val signature = bs.readBinary()
      // TODO: validate signature with hash
    }

    Blob(id, hash._1._2)
  }

  def readHashed[T](stream: InputStream, hashAlgorithm: HashAlgorithm)(inner: InputStream => T): (T, Array[Byte]) = {
    var result: Option[T] = None

    val hash = stream.hashed(hashAlgorithm.algorithmName) { hs =>
      result = Some(inner(hs))
    }

    return (result.get, hash)
  }

  def writeHashed(stream: OutputStream, hashAlgorithm: HashAlgorithm)(inner: OutputStream => Any): Array[Byte] = {
    stream.hashed(hashAlgorithm.algorithmName) { hs =>
      inner(hs)
    }
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

  def readIssuerIdentityBlock(stream: InputStream): Issuer = {
    readBlock(stream, IssuerIdentityBlockType) { bs =>
      Issuer(bs.readBinary(), bs.readString())
    }
  }

  def writeIssuerIdentityBlock(stream: OutputStream, issuer: Issuer) {
    writeBlock(stream, IssuerIdentityBlockType) { bs => 
      bs.writeBinary(issuer.id)
      bs.writeString(issuer.name)
    }
  }

  def readBlock[T](stream: InputStream, expectedBlockType: BlockType)(inner: InputStream => T): T = {
    var result: Option[T] = None

    stream.preSizedInner(stream.readInt64()) { is =>
      val actualBlockType = blockTypeMapInverse(is.readInt8())
      assert(s"Expected block of type '${expectedBlockType.getClass.getSimpleName}'", expectedBlockType == actualBlockType)
      result = Some(inner(is))
    }

    return result.get
  }

  def writeBlock(stream: OutputStream, blockType: BlockType)(inner: OutputStream => Any) {
    stream.cached(cs => stream.writeInt64(cs.size)) { cs =>
      cs.writeInt8(blockTypeMap(blockType))
      inner(cs)
    }
  }

  def writeBlock(stream: OutputStream, blockType: BlockType, innerSize: Long)(inner: OutputStream => Any) {
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