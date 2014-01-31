package net.secloud.core.utils

import java.io._

object StreamUtils {
  def bytesAsStream[T](bytes: Array[Byte])(inner: InputStream => T): T = {
    val s = new ByteArrayInputStream(bytes)
    val result = inner(s)
    s.close()
    result
  }

  def streamAsBytes(inner: OutputStream => Any): Array[Byte] = {
    val s = new ByteArrayOutputStream()
    inner(s)
    s.flush()
    val result = s.toByteArray
    s.close()
    result
  }
}