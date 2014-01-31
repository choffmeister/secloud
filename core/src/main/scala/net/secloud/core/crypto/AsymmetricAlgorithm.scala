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
  def wrapKey(plainKey: Array[Byte]): Array[Byte]
  def unwrapKey(wrappedKey: Array[Byte]): Array[Byte]
}
