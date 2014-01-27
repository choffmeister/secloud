package net.secloud.core.security

import java.io.{InputStream, OutputStream}
import java.security.SecureRandom
import javax.crypto._
import javax.crypto.spec._
import net.secloud.core.utils.BinaryReaderWriter._

sealed abstract class SymmetricMode
case object EncryptMode extends SymmetricMode
case object DecryptMode extends SymmetricMode

case class SymmetricParams(algorithm: SymmetricAlgorithm, key: Option[SecretKey], iv: Option[IvParameterSpec])

abstract class SymmetricAlgorithm {
  val friendlyName: String
  val algorithmName: String
  val blockModeName: String
  val paddingName: String
  def fullAlgorithmName: String = s"$algorithmName/$blockModeName/$paddingName"

  val blockSize: Int
  val keySize: Int
  def blockSizeBits: Int = blockSize * 8
  def keySizeBits: Int = keySize * 8

  def supported: Boolean
  def encryptedSize(plainSize: Long): Long

  def createCipher(mode: SymmetricMode, params: SymmetricParams): Cipher = {
    val cipher = Cipher.getInstance(fullAlgorithmName)
    val mode2 = mode match {
      case EncryptMode => Cipher.ENCRYPT_MODE
      case DecryptMode => Cipher.DECRYPT_MODE
    }
    cipher.init(mode2, params.key.getOrElse(null), params.iv.getOrElse(null))
    cipher
  }

  def generateParameters(): SymmetricParams = {
    val random = new SecureRandom()
    val keyGen = KeyGenerator.getInstance(algorithmName)
    keyGen.init(keySizeBits, random)
    val key = keyGen.generateKey()
    val ivBinary = new Array[Byte](blockSize)
    random.nextBytes(ivBinary)
    val params = new IvParameterSpec(ivBinary)

    SymmetricParams(this, Some(key), Some(params))
  }

  def readParameters(stream: InputStream): SymmetricParams = {
    val keyBinary = stream.readBinary()
    val key = new SecretKeySpec(keyBinary, 0, keyBinary.length, algorithmName)
    val ivBinary = stream.readBinary()
    val params = new IvParameterSpec(ivBinary)

    SymmetricParams(this, Some(key), Some(params))
  }

  def writeParameters(stream: OutputStream, parameters: SymmetricParams) {
    stream.writeBinary(parameters.key.get.getEncoded)
    stream.writeBinary(parameters.iv.get.getIV)
  }

  def wrapStream(stream: InputStream, parameters: SymmetricParams): CipherInputStream = {
    new CipherInputStream(stream, createCipher(DecryptMode, parameters))
  }

  def wrapStream(stream: OutputStream, parameters: SymmetricParams): CipherOutputStream = {
    new CipherOutputStream(stream, createCipher(EncryptMode, parameters))
  }
}

object NullEncryption extends SymmetricAlgorithm {
  val friendlyName = "NULL"
  val algorithmName = "NULL"
  val blockModeName = "NULL"
  val paddingName = "NoPadding"
  override def fullAlgorithmName: String = "NULL"

  val blockSize = 1
  val keySize = 0

  def supported = true
  def encryptedSize(plainSize: Long) = plainSize

  override def createCipher(mode: SymmetricMode, params: SymmetricParams): Cipher = new NullCipher()

  override def generateParameters(): SymmetricParams = SymmetricParams(this, None, None)

  override def readParameters(stream: InputStream): SymmetricParams = SymmetricParams(this, None, None)

  override def writeParameters(stream: OutputStream, parameters: SymmetricParams) {}
}
