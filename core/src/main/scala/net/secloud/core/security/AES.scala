package net.secloud.core.security

import javax.crypto.Cipher

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
