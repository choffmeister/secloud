package net.secloud.core.crypto

import java.io._
import java.math.BigInteger
import java.security._
import net.secloud.core.utils._
import net.secloud.core.utils.StreamUtils._
import org.bouncycastle.asn1.x509._
import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.CipherParameters
import org.bouncycastle.crypto.digests.SHA512Digest
import org.bouncycastle.crypto.encodings.PKCS1Encoding
import org.bouncycastle.crypto.engines.{RSAEngine, RSABlindedEngine}
import org.bouncycastle.crypto.generators.KDF2BytesGenerator
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator
import org.bouncycastle.crypto.kems.RSAKeyEncapsulation
import org.bouncycastle.crypto.params._
import org.bouncycastle.crypto.util._
import org.bouncycastle.openssl._
import org.bouncycastle.util.Arrays
import org.bouncycastle.asn1.pkcs._

class RSA(keyPair: AsymmetricCipherKeyPair) extends AsymmetricAlgorithmInstance {
  private val pub: RSAKeyParameters = keyPair.getPublic.asInstanceOf[RSAKeyParameters]
  private val priv: Option[RSAKeyParameters] = Option(keyPair.getPrivate.asInstanceOf[RSAKeyParameters])

  val algorithm = RSA
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

  def signHash(hash: Array[Byte]): Array[Byte] = {
    val cipher = new PKCS1Encoding(new RSABlindedEngine())
    cipher.init(true, priv.get)
    cipher.processBlock(hash, 0, hash.length)
  }

  def validateHash(hash: Array[Byte], signature: Array[Byte]): Boolean = {
    val cipher = new PKCS1Encoding(new RSABlindedEngine())
    cipher.init(false, pub)

    try {
      val signature2 = cipher.processBlock(signature, 0, signature.length)

      Arrays.constantTimeAreEqual(hash, signature2)
    } catch {
      case e: Throwable => false
    }
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

object RSA extends AsymmetricAlgorithm {
  def generate(strength: Int) = generate(strength, 75)
  def generate(strength: Int, certainty: Int): AsymmetricAlgorithmInstance = {
    val random = RandomGenerator.random
    val generator = new RSAKeyPairGenerator()
    generator.init(new RSAKeyGenerationParameters(BigInteger.valueOf(0x10001), random, strength, certainty))
    val keyPair = generator.generateKeyPair()

    new RSA(keyPair)
  }

  def save(output: OutputStream, key: AsymmetricAlgorithmInstance, includePrivate: Boolean): Unit = {
    val rsa = key.asInstanceOf[RSA]

    val publicKeyEncoded = SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(rsa.pub).toASN1Primitive().getEncoded("DER")
    val privateKeyEncoded = if (includePrivate) {
      PrivateKeyInfoFactory.createPrivateKeyInfo(rsa.priv.get).toASN1Primitive.getEncoded("DER")
    } else Array.empty[Byte]

    output.writeBinary(publicKeyEncoded)
    output.writeBinary(privateKeyEncoded)
  }

  def load(input: InputStream): AsymmetricAlgorithmInstance = {
    val publicKeyEncoded = input.readBinary()
    val privateKeyEncoded = input.readBinary()

    val publicKey = PublicKeyFactory.createKey(publicKeyEncoded)
    val privateKey = if (privateKeyEncoded.length > 0) {
      Some(PrivateKeyFactory.createKey(privateKeyEncoded))
    } else None

    new RSA(new AsymmetricCipherKeyPair(publicKey, privateKey.orNull))
  }

  def fingerprint(key: AsymmetricAlgorithmInstance): Array[Byte] = {
    val publicKeyEncoded = streamAsBytes(bs => save(bs, key, false))
    SHA1.create().hash(publicKeyEncoded)
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

  def saveToPEM(output: OutputStream, rsa: RSA, includePrivate: Boolean): Unit = {
    val streamWriter = new OutputStreamWriter(output)
    val pemWriter = new PEMWriter(streamWriter)

    if (includePrivate) {
      val privateKey = PrivateKeyInfoFactory.createPrivateKeyInfo(rsa.priv.get)
      pemWriter.writeObject(privateKey)
    } else {
      val publicKey = SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(rsa.pub)
      pemWriter.writeObject(publicKey)
    }

    pemWriter.close()
  }
}
