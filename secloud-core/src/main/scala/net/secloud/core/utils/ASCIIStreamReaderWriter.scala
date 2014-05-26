package net.secloud.core.utils

import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import net.secloud.core.objects.ObjectId

class ASCIIStreamWriter(val stream: OutputStream) extends AnyVal with StreamWriter {
  def writeInt8(value: Byte): Unit = writeString(value.toString)

  def writeInt16(value: Short): Unit = writeString(value.toString)

  def writeInt32(value: Int): Unit = writeString(value.toString)

  def writeInt64(value: Long): Unit = writeString(value.toString)

  def writeInt7(value: Long): Unit = writeString(value.toString)

  def writeBoolean(value: Boolean): Unit = writeString(value.toString)

  def writeString(value: String): Unit = writeBinary(value.getBytes("UTF-8"))

  def writeBinary(value: Seq[Byte]): Unit = writeBinary(value.toArray)

  def writeBinary(value: Array[Byte]): Unit = {
    StreamUtils.writeBytes(stream, value.length.toString.getBytes("ASCII"))
    StreamUtils.writeBytes(stream, ":".getBytes("ASCII"))
    StreamUtils.writeBytes(stream, value)
    StreamUtils.writeBytes(stream, "\n".getBytes("ASCII"))
  }

  def writeObjectId(value: ObjectId): Unit = writeString(value.toString)

  def writeList[T](value: List[T])(inner: T ⇒ Any): Unit = {
    writeInt7(value.length)
    value.foreach(item ⇒ inner(item))
  }

  def writeMap[A, B](value: Map[A, B])(inner: (A, B) ⇒ Any): Unit = {
    writeInt7(value.size)
    value.foreach(item ⇒ inner(item._1, item._2))
  }

  def writeStream(inner: OutputStream ⇒ Any): Unit = {
    val bs = new BlockOutputStream(stream, ownsInner = false)
    inner(bs)
    bs.close()
  }

  def close(): Unit = stream.close()
}

class ASCIIStreamReader(val stream: InputStream) extends AnyVal with StreamReader {
  def readInt8(): Byte = readString().toByte

  def readInt16(): Short = readString().toShort

  def readInt32(): Int = readString().toInt

  def readInt64(): Long = readString().toLong

  def readInt7(): Long = readString().toLong

  def readBoolean(): Boolean = readString().toBoolean

  def readString(): String = new String(readBinary(), "UTF-8")

  def readBinary(): Array[Byte] = {
    val sbuf = Array[Byte](1)
    var s = ""
    while (s.length <= 0 || s(s.length - 1) != ':') {
      stream.read(sbuf, 0, 1)
      s += new String(sbuf, "ASCII")
    }
    s = s.substring(0, s.length - 1)

    val length = s.toInt
    val buf = new Array[Byte](length)
    StreamUtils.readBytes(stream, buf, 0, length)
    stream.read(sbuf, 0, 1)
    return buf
  }

  def readObjectId(): ObjectId = return ObjectId(readString())

  def readList[T]()(inner: ⇒ T): List[T] = {
    (1 to readInt7().toInt).map(i ⇒ inner).toList
  }

  def readMap[A, B]()(inner: ⇒ (A, B)): Map[A, B] = {
    (1 to readInt7().toInt).map(i ⇒ inner).toMap
  }

  def readStream[T](inner: InputStream ⇒ T): T = {
    val bs = new BlockInputStream(stream, ownsInner = false)
    val result = inner(bs)
    bs.close()
    result
  }

  def close(): Unit = stream.close()
}
