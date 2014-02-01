package net.secloud.core.crypto

trait AsymmetricAlgorithm {
  def generate(strength: Int): AsymmetricAlgorithmInstance
}

trait AsymmetricAlgorithmInstance {
  val algorithm: AsymmetricAlgorithm
  val name: String

  def isPublic: Boolean
  def isPrivate: Boolean

  def encrypt(plainBytes: Array[Byte]): Array[Byte]
  def decrypt(encryptedBytes: Array[Byte]): Array[Byte]
  def signHash(hash: Array[Byte]): Array[Byte]
  def validateHash(hash: Array[Byte], signature: Array[Byte]): Boolean
  def wrapKey(plainKey: Array[Byte]): Array[Byte]
  def unwrapKey(wrappedKey: Array[Byte]): Array[Byte]
}
