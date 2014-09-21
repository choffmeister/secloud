package net.secloud.core.crypto

import java.io.{ InputStream, OutputStream }

trait SymmetricAlgorithm {
  def generate(keySize: Int): SymmetricAlgorithmInstance
  def save(output: OutputStream, instance: SymmetricAlgorithmInstance): Unit
  def load(input: InputStream): SymmetricAlgorithmInstance
}

trait SymmetricAlgorithmInstance {
  val algorithm: SymmetricAlgorithm
  val name: String
  val keySize: Int
  val blockSize: Int

  def encrypt(output: OutputStream)(inner: OutputStream ⇒ Any): Unit
  def decrypt[T](input: InputStream)(inner: InputStream ⇒ T): T
}
