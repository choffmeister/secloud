package net.secloud.core.crypto

import java.io._

trait HashAlgorithm {
  def create(): HashAlgorithmInstance
}

trait HashAlgorithmInstance {
  val algorithm: HashAlgorithm
  val name: String

  def hash(output: OutputStream)(inner: OutputStream => Any): Array[Byte]
  def hash[T](input: InputStream)(inner: InputStream => T): (Array[Byte], T)
}
