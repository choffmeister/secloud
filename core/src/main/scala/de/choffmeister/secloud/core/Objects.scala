package de.choffmeister.secloud.core

import java.io.OutputStream
import java.io.InputStream
import de.choffmeister.secloud.core.utils._
import de.choffmeister.secloud.core.utils.RichStream._
import de.choffmeister.secloud.core.utils.BinaryReaderWriter._
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream

case class Issuer(id: Seq[Byte], name: String)

object Issuer {
  def apply(): Issuer = Issuer(Seq.empty[Byte], "")
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
    val os = new ObjectOutputStream(stream, `SHA-2-256`, `AES-256`, `AES-256`.generateKey())

    writeHeader(os, obj.objectType)
    writeIssuerIdentityBlock(os, obj.issuer)

    // public block
    writeBlock(os, PublicBlockType) { bs =>
    }

    // private block
    writeBlock(os, PrivateBlockType) { bs =>
    }

    // TODO: sign hash
    val signature = os.hash.get
    writeIssuerSignatureBlock(os, signature)
  }

  def deserialize(id: ObjectId, stream: InputStream): BaseObject = {
    val os = new ObjectInputStream(stream, `SHA-2-256`, `AES-256`, `AES-256`.generateKey())
    val objectType = readHeader(os)
    val issuer = readIssuerIdentityBlock(os)

    // public block
    val publicBlock = readBlock(os, PublicBlockType) { bs =>
    }

    // private block
    val privateBlock = readBlock(os, PrivateBlockType) { bs =>
    }

    // issuer signature block
    val signature = readIssuerSignatureBlock(os)

    // TODO: validate signature with hash
    assert("Signature invalid", signature == os.hash.get)

    Blob(id, issuer)
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

  def readBlock[T](stream: InputStream, expectedBlockType: BlockType)(inner: InputStream => T): T = {
    val actualBlockType = blockTypeMapInverse(stream.readInt8())
    assert(s"Expected block of type '${expectedBlockType.getClass.getSimpleName}'", expectedBlockType == actualBlockType)
    
    stream.preSizedInner(stream.readInt64()) { is =>
      inner(is)
    }
  }

  def writeBlock(stream: OutputStream, blockType: BlockType)(inner: OutputStream => Any) {
    stream.writeInt8(blockTypeMap(blockType))
    stream.cached(cs => stream.writeInt64(cs.size)) { cs =>
      inner(cs)
    }
  }

  def writeBlock(stream: OutputStream, blockType: BlockType, innerSize: Long)(inner: OutputStream => Any) {
    stream.writeInt8(blockTypeMap(blockType))
    stream.writeInt64(innerSize)
    stream.preSizedInner(innerSize)(inner)
  }

  def assert(errorMessage: String, cond: Boolean): Unit = assert(errorMessage)(cond == true)

  def assert(errorMessage: String)(cond: => Boolean): Unit = {
    if (cond == false) {
      throw new ObjectSerializationException(errorMessage)
    }
  }
}