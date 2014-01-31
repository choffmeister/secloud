package net.secloud.core.crypto

import java.io._
import java.math.BigInteger
import java.security._
import net.secloud.core.utils.BinaryReaderWriter._
import net.secloud.core.utils.StreamUtils._
import org.bouncycastle.asn1.x509._
import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.CipherParameters
import org.bouncycastle.crypto.digests.SHA512Digest
import org.bouncycastle.crypto.encodings.PKCS1Encoding
import org.bouncycastle.crypto.engines.RSAEngine
import org.bouncycastle.crypto.generators.KDF2BytesGenerator
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator
import org.bouncycastle.crypto.kems.RSAKeyEncapsulation
import org.bouncycastle.crypto.params._
import org.bouncycastle.crypto.util._
import org.bouncycastle.openssl._

class RSA(keyPair: AsymmetricCipherKeyPair) extends AsymmetricAlgorithm {
  private val pub: RSAKeyParameters = keyPair.getPublic.asInstanceOf[RSAKeyParameters]
  private val priv: Option[RSAKeyParameters] = Option(keyPair.getPrivate.asInstanceOf[RSAKeyParameters])

  val name = s"RSA-${pub.getModulus.bitLength}"
  def isPublic = true
  def isPrivate = !priv.isEmpty

  def encrypt(plainBytes: Array[Byte]): Array[Byte] = {
    val cipher = new PKCS1Encoding(new RSAEngine())
    cipher.init(true, pub)
    cipher.processBlock(plainBytes, 0, plainBytes.length)
  }

  def decrypt(encryptedBytes: Array[Byte]): Array[Byte] = {
    val cipher = new PKCS1Encoding(new RSAEngine())
    cipher.init(false, priv.get)
    cipher.processBlock(encryptedBytes, 0, encryptedBytes.length)
  }

  def wrapKey(plainKey: Array[Byte]): Array[Byte] = {
    // initialize key derivation function KDF2 with SHA512
    val digest = new SHA512Digest()
    val kdf = new KDF2BytesGenerator(digest)

    // initialize RSA-KEM
    val enc = new RSAKeyEncapsulation(kdf, RandomGenerator.random)
    enc.init(pub)

    // generate and wrap random key
    val wrappedRandomKey = new Array[Byte]((pub.getModulus.bitLength + 7) / 8)
    val randomKey = enc.encrypt(wrappedRandomKey, 0, plainKey.length).asInstanceOf[KeyParameter].getKey

    // calculate bitmask to get key from random key
    val bitmask = randomKey.zip(plainKey).map(b => b._1 ^ b._2).map(_.toByte).toArray

    // write as byte stream
    streamAsBytes { s =>
      s.writeBinary(wrappedRandomKey)
      s.writeBinary(bitmask)
    }
  }

  def unwrapKey(wrappedKey: Array[Byte]): Array[Byte] = {
    // initialize key derivation function KDF2 with SHA512
    val digest = new SHA512Digest()
    val kdf = new KDF2BytesGenerator(digest)

    // initialize RSA-KEM
    val enc = new RSAKeyEncapsulation(kdf, RandomGenerator.random)
    enc.init(priv.get)

    // read from byte stream
    val (wrappedRandomKey, bitmask) = bytesAsStream(wrappedKey) { s =>
      val k1 = s.readBinary()
      val k2 = s.readBinary()
      (k1, k2)
    }

    // unwrap random key
    val randomKey = enc.decrypt(wrappedRandomKey, bitmask.length).asInstanceOf[KeyParameter].getKey

    // calculate key from random key and bitmask
    val key = randomKey.zip(bitmask).map(b => b._1 ^ b._2).map(_.toByte).toArray

    key
  }
}

object RSA {
  def generate(strength: Int, certainty: Int): RSA = {
    val random = RandomGenerator.random
    val generator = new RSAKeyPairGenerator()
    generator.init(new RSAKeyGenerationParameters(BigInteger.valueOf(0x10001), random, strength, certainty))
    val keyPair = generator.generateKeyPair()

    new RSA(keyPair)
  }

  def loadFromPEM(input: InputStream, password: Option[Array[Char]] = None): RSA = {
    val streamReader = new InputStreamReader(input)
    val pemReader = new PEMParser(streamReader)
    val pem = pemReader.readObject() match {
      case pem: PEMEncryptedKeyPair =>
        // TODO: decrypt without using the JCE
        val decryptor = new org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder().build(password.get)
        pem.decryptKeyPair(decryptor)
      case o => o
    }
    pemReader.close()

    val (pub, priv) = pem match {
      case pem: PEMKeyPair =>
        val pub = PublicKeyFactory.createKey(pem.getPublicKeyInfo)
        val priv = Some(PrivateKeyFactory.createKey(pem.getPrivateKeyInfo))
        (pub, priv)
      case pem: SubjectPublicKeyInfo =>
        val pub = PublicKeyFactory.createKey(pem)
        (pub, None)
      case o => throw new Exception(s"Unsupported object ${o.getClass} in PEM file")
    }

    new RSA(new AsymmetricCipherKeyPair(pub, priv.orNull))
  }
}
