package net.secloud.core.crypto

import java.io._

trait HashAlgorithm {
  def create(): HashAlgorithmInstance
}

trait HashAlgorithmInstance {
  val algorithm: HashAlgorithm
  val name: String

  def hash(output: OutputStream)(inner: OutputStream ⇒ Any): Array[Byte]
  def hash[T](input: InputStream)(inner: InputStream ⇒ T): (Array[Byte], T)

  def hash(bytes: Array[Byte]): Array[Byte] = {
    val bs = new ByteArrayOutputStream()
    hash(bs)(hs ⇒ hs.write(bytes, 0, bytes.length))
  }
}
