package net.secloud.core.security

import java.io.{InputStream, OutputStream, ByteArrayInputStream, ByteArrayOutputStream}
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.Cipher
import javax.crypto.NullCipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.SecretKeySpec
import net.secloud.core.utils.BinaryReaderWriter._
import java.security.SecureRandom
import java.security.spec.AlgorithmParameterSpec
import javax.crypto.spec.IvParameterSpec
import java.security.DigestInputStream
import java.security.DigestOutputStream
import java.security.MessageDigest

object CryptographicAlgorithms {
  sealed abstract class SymmetricMode
  case object EncryptMode extends SymmetricMode
  case object DecryptMode extends SymmetricMode

  case class SymmetricParams(algorithm: SymmetricAlgorithm, key: Option[SecretKey], iv: Option[IvParameterSpec])

  sealed abstract class SymmetricAlgorithm {
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

  sealed abstract class AES extends SymmetricAlgorithm {
    val algorithmName = "AES"
    val blockModeName = "CBC"
    val paddingName = "PKCS5Padding"
    val blockSize = 16

    def supported = Cipher.getMaxAllowedKeyLength("AES") >= keySizeBits
    def encryptedSize(plainSize: Long) = (plainSize / 16L + 1L) * 16L
  }

  case object `AES-128` extends AES {
    val friendlyName = "AES-128"
    val keySize = 16
  }

  case object `AES-192` extends AES {
    val friendlyName = "AES-192"
    val keySize = 24
  }

  case object `AES-256` extends AES {
    val friendlyName = "AES-256"
    val keySize = 32
  }

  sealed abstract class HashAlgorithm {
    val friendlyName: String
    val algorithmName: String

    def wrapStream(stream: InputStream): DigestInputStream = {
      val digest = MessageDigest.getInstance(algorithmName)
      new DigestInputStream(stream, digest)
    }

    def wrapStream(stream: OutputStream): DigestOutputStream = {
      val digest = MessageDigest.getInstance(algorithmName)
      new DigestOutputStream(stream, digest)
    }
  }

  case object `SHA-1` extends HashAlgorithm {
    val friendlyName = "SHA-1"
    val algorithmName = "SHA-1"
  }

  sealed abstract class `SHA-2` extends HashAlgorithm

  case object `SHA-2-256` extends `SHA-2` {
    val friendlyName = "SHA-2-256"
    val algorithmName = "SHA-256"
  }

  case object `SHA-2-384` extends `SHA-2` {
    val friendlyName = "SHA-2-384"
    val algorithmName = "SHA-384"
  }

  case object `SHA-2-512` extends `SHA-2` {
    val friendlyName = "SHA-2-512"
    val algorithmName = "SHA-512"
  }
}

object CryptographicAlgorithmSerializerConstants {
  import CryptographicAlgorithms._

  val symmetricAlgorithmMap = Map[SymmetricAlgorithm, Byte](
    NullEncryption -> 0x00,
    `AES-128` -> 0x01,
    `AES-192` -> 0x02,
    `AES-256` -> 0x03
  )
  val symmetricAlgorithmMapInverse = symmetricAlgorithmMap.map(entry => (entry._2, entry._1))

  val hashAlgorithmMap = Map[HashAlgorithm, Byte](
    `SHA-1`-> 0x00,
    `SHA-2-256` -> 0x01,
    `SHA-2-384` -> 0x02,
    `SHA-2-512` -> 0x03
  )
  val hashAlgorithmMapInverse = hashAlgorithmMap.map(entry => (entry._2, entry._1))
}

object CryptographicAlgorithmSerializer {
  import CryptographicAlgorithms._
  import CryptographicAlgorithmSerializerConstants._

  def writeSymmetricParams(stream: OutputStream, parameters: SymmetricParams) {
    stream.writeInt8(symmetricAlgorithmMap(parameters.algorithm))
    parameters.algorithm.writeParameters(stream, parameters)
  }

  def readSymmetricParams(stream: InputStream): SymmetricParams = {
    val algorithm = symmetricAlgorithmMapInverse(stream.readInt8())
    algorithm.readParameters(stream)
  }

  def writeHashAlgorithm(stream: OutputStream, hashAlgorithm: HashAlgorithm) {
    stream.writeInt8(hashAlgorithmMap(hashAlgorithm))
  }

  def readHashAlgorithm(stream: InputStream): HashAlgorithm = {
    hashAlgorithmMapInverse(stream.readInt8())
  }
}
