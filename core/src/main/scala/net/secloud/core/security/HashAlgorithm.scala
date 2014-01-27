package net.secloud.core.security

import java.io.{InputStream, OutputStream}
import java.security.MessageDigest
import java.security.{DigestInputStream, DigestOutputStream}

abstract class HashAlgorithm {
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
