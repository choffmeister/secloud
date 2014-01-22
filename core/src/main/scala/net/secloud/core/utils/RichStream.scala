package net.secloud.core.utils

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.security.DigestInputStream
import java.security.DigestOutputStream
import scala.language.implicitConversions

class RichInputStream(val stream: InputStream) {
  def hashed(hashAlgorithmName: String)(inner: InputStream => Any): Array[Byte] = {
    val digest = MessageDigest.getInstance(hashAlgorithmName)
    val hs = new DigestInputStream(stream, digest)
    inner(hs)
    digest.digest()
  }

  def pipeTo(target: OutputStream): Unit = {
    val buf = new Array[Byte](1024)
    var done = false

    while (!done) {
      val read = stream.read(buf, 0, buf.length)
      if (read > 0) target.write(buf, 0, read)
      else done = true
    }
  }
}

class RichOutputStream(val stream: OutputStream) {
  def cached(after: ByteArrayOutputStream => Any)(inner: ByteArrayOutputStream => Any) {
    val cache = new ByteArrayOutputStream()
    inner(cache)
    after(cache)
    val buf = cache.toByteArray
    stream.write(buf)
  }

  def hashed(hashAlgorithmName: String)(inner: OutputStream => Any): Array[Byte] = {
    val digest = MessageDigest.getInstance(hashAlgorithmName)
    val hs = new DigestOutputStream(stream, digest)
    inner(hs)
    digest.digest()
  }
}

object RichStream {
  implicit def inputStreamToRichInputStream(stream: InputStream) = new RichInputStream(stream)
  implicit def outputStreamToRichOutputStream(stream: OutputStream) = new RichOutputStream(stream)
}
