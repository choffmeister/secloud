package net.secloud.core.utils

import java.io._

object StreamUtils {
  def pipeStream(source: InputStream, target: OutputStream): Unit = {
    val buf = new Array[Byte](1024)
    var done = false

    while (!done) {
      val read = source.read(buf, 0, buf.length)
      if (read > 0) target.write(buf, 0, read)
      else done = true
    }
  }

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

  def writeBytes(output: OutputStream, bytes: Array[Byte]): Unit = {
    output.write(bytes, 0, bytes.length)
  }

  def readBytes(input: InputStream): Array[Byte] = {
    val bs = new ByteArrayOutputStream()
    val buf = new Array[Byte](8192)
    var done = false

    while (!done) {
      val read = input.read(buf, 0, buf.length)
      if (read > 0) {
        bs.write(buf, 0, read)
      } else done = true
    }

    bs.toByteArray
  }

  def writeBytes(stream: OutputStream, buffer: Array[Byte], offset: Int, length: Int) {
    stream.write(buffer, offset, length)
  }

  @scala.annotation.tailrec
  def readBytes(stream: InputStream, buffer: Array[Byte], offset: Int, length: Int) {
    if (length > 0) {
      val read = stream.read(buffer, offset, length)
      if (read <= 0) throw new EOFException()
      readBytes(stream, buffer, offset + read, length - read)
    }
  }

  def writeString(output: OutputStream, str: String): Unit = {
    writeBytes(output, str.getBytes("UTF-8"))
  }

  def readString(input: InputStream): String = {
    new String(readBytes(input), "UTF-8")
  }

  def writeBytesToFile(file: File, bytes: Array[Byte]): Unit = {
    val fs = new FileOutputStream(file)
    try {
      writeBytes(fs, bytes)
    } finally {
      fs.close()
    }
  }

  def readBytesFromFile(file: File): Array[Byte] = {
    val fs = new FileInputStream(file)
    try {
      readBytes(fs)
    } finally {
      fs.close()
    }
  }
}
