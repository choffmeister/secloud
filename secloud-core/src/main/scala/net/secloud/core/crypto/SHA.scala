package net.secloud.core.crypto

import org.bouncycastle.crypto.digests._

class SHA1 extends BouncyCastleHashAlgorithmInstance(new SHA1Digest()) {
  val algorithm = SHA1
  val name = "SHA-1"
}

object SHA1 extends HashAlgorithm {
  def create() = new SHA1()
}

class SHA256 extends BouncyCastleHashAlgorithmInstance(new SHA256Digest()) {
  val algorithm = SHA256
  val name = "SHA-256"
}

object SHA256 extends HashAlgorithm {
  def create() = new SHA256()
}

class SHA384 extends BouncyCastleHashAlgorithmInstance(new SHA384Digest()) {
  val algorithm = SHA384
  val name = "SHA-384"
}

object SHA384 extends HashAlgorithm {
  def create() = new SHA384()
}

class SHA512 extends BouncyCastleHashAlgorithmInstance(new SHA512Digest()) {
  val algorithm = SHA512
  val name = "SHA-512"
}

object SHA512 extends HashAlgorithm {
  def create() = new SHA512()
}
