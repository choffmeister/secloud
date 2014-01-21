package net.secloud.core.security

import java.io.InputStream
import java.io.OutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.Cipher
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
  case class SymmetricEncryptionParameters(algorithm: SymmetricEncryptionAlgorithm, key: SecretKey, iv: IvParameterSpec)

  sealed abstract class SymmetricEncryptionAlgorithm {
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

    def generateKey(): SymmetricEncryptionParameters = {
      val random = new SecureRandom()
      val keyGen = KeyGenerator.getInstance(algorithmName)
      keyGen.init(keySizeBits, random)
      val key = keyGen.generateKey()
      val ivBinary = new Array[Byte](blockSize)
      random.nextBytes(ivBinary)
      val params = new IvParameterSpec(ivBinary)

      SymmetricEncryptionParameters(this, key, params)
    }

    def readParameters(stream: InputStream): SymmetricEncryptionParameters = {
      val keyBinary = stream.readBinary()
      val key = new SecretKeySpec(keyBinary, 0, keyBinary.length, algorithmName)
      val ivBinary = stream.readBinary()
      val params = new IvParameterSpec(ivBinary)

      SymmetricEncryptionParameters(this, key, params)
    }

    def writeParameters(stream: OutputStream, parameters: SymmetricEncryptionParameters) {
      stream.writeBinary(parameters.key.getEncoded)
      stream.writeBinary(parameters.iv.getIV)
    }

    def wrapStream(stream: InputStream, parameters: SymmetricEncryptionParameters): CipherInputStream = {
      val cipher = Cipher.getInstance(fullAlgorithmName);
      cipher.init(Cipher.DECRYPT_MODE, parameters.key, parameters.iv);
      new CipherInputStream(stream, cipher);
    }

    def wrapStream(stream: OutputStream, parameters: SymmetricEncryptionParameters): CipherOutputStream = {
      val cipher = Cipher.getInstance(fullAlgorithmName);
      cipher.init(Cipher.ENCRYPT_MODE, parameters.key, parameters.iv);
      new CipherOutputStream(stream, cipher);
    }
  }

  sealed abstract class AES extends SymmetricEncryptionAlgorithm {
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
