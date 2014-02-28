package net.secloud.core.objects

import com.jcraft.jzlib.{GZIPInputStream, GZIPOutputStream}
import java.io.InputStream
import java.io.OutputStream
import net.secloud.core.crypto._
import net.secloud.core.utils._

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

  def readSignatureBlock(stream: InputStream): (Seq[Byte], Seq[Byte], Seq[Byte]) = {
    readBlock(stream, SignatureBlockType) { bs =>
      val issuerFingerprint = bs.readBinary().toSeq
      val hash = bs.readBinary().toSeq
      val signature = bs.readBinary().toSeq
      (issuerFingerprint, hash, signature)
    }
  }

  def writeSignatureBlock(stream: OutputStream, issuerFingerprint: Seq[Byte], hash: Seq[Byte], signature: Seq[Byte]) {
    writeBlock(stream, SignatureBlockType) { bs =>
      bs.writeBinary(issuerFingerprint)
      bs.writeBinary(hash)
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

  def readPrivateBlock[T](stream: InputStream, key: SymmetricAlgorithmInstance)(inner: InputStream => T): T = {
    readBlock(stream, PrivateBlockType) { bs =>
      key.decrypt(bs)(inner(_))
    }
  }

  def writePrivateBlock(stream: OutputStream, key: SymmetricAlgorithmInstance)(inner: OutputStream => Any): Unit = {
    writeBlock(stream, PrivateBlockType) { bs =>
      key.encrypt(bs)(inner(_))
    }
  }

  def readBlock[T](stream: InputStream, expectedBlockType: BlockType)(inner: InputStream => T): T = {
    val actualBlockType = blockTypeMapInverse(stream.readInt8())
    assert(s"Expected block of type '${expectedBlockType.getClass.getSimpleName}'", expectedBlockType == actualBlockType)
    stream.readStream(inner)
  }

  def writeBlock(stream: OutputStream, blockType: BlockType)(inner: OutputStream => Any) {
    stream.writeInt8(blockTypeMap(blockType))
    stream.writeStream(inner)
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

  def signObject(output: OutputStream, privateKey: AsymmetricAlgorithmInstance)(inner: OutputStream => Any): ObjectId = {
    def hash(os: OutputStream)(inner: OutputStream => Any): Array[Byte] = SHA1.create().hash(os)(hs => inner(hs))

    val totalHash = hash(output) { ths =>
      val signatureHash = hash(ths)(inner)

      writeSignatureBlock(ths, privateKey.fingerprint, signatureHash, privateKey.signHash(signatureHash))
    }

    output.writeBinary(totalHash)
    output.flush()

    return ObjectId(totalHash)
  }

  def validateObject[T](input: InputStream, publicKeys: Map[Seq[Byte], AsymmetricAlgorithmInstance])(inner: InputStream => T): T = {
    def hash(is: InputStream)(inner: InputStream => T): (Array[Byte], T) = SHA1.create().hash(is)(hs => inner(hs))

    val (totalHash, result) = hash(input) { ths =>
      val (signatureHash, result) = hash(ths)(inner)

      val (readIssuerFingerprint, readSignatureHash, readSignature) = readSignatureBlock(ths)
      assert("Unknown issuer fingerprint", publicKeys.contains(readIssuerFingerprint) == true)
      val publicKey = publicKeys(readIssuerFingerprint)
      assert("Invalid signature", publicKey.validateHash(readSignatureHash.toArray, readSignature.toArray) == true)

      result
    }

    val readTotalHash = input.readBinary()
    assert("Invalid object hash", readTotalHash.toSeq == totalHash.toSeq)
    assert("Additional data behind object", input.read() < 0)

    return result
  }

  def assert(errorMessage: String, cond: => Boolean): Unit = {
    if (!cond) {
      throw new ObjectSerializationException(errorMessage)
    }
  }
}
