package net.secloud.core.utils

import java.io.OutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import net.secloud.core.objects.ObjectId
import scala.language.implicitConversions
import scala.annotation.tailrec

class BinaryWriter(val stream: OutputStream) {
  private val bufRaw = new Array[Byte](8)
  private val buf = ByteBuffer.wrap(bufRaw)
  buf.order(ByteOrder.BIG_ENDIAN)

  def writeInt8(value: Byte): Unit = {
    buf.put(0, value)
    writeToStream(stream, bufRaw, 0, 1)
  }

  def writeInt16(value: Short): Unit = {
    buf.putShort(0, value)
    writeToStream(stream, bufRaw, 0, 2)
  }

  def writeInt32(value: Int): Unit = {
    buf.putInt(0, value)
    writeToStream(stream, bufRaw, 0, 4)
  }

  def writeInt64(value: Long): Unit = {
    buf.putLong(0, value)
    writeToStream(stream, bufRaw, 0, 8)
  }

  def writeInt7(value: Long): Unit = {
    assert(value >= 0L)
    assert(value < 72057594037927936L)

    if (value > 0L) {
      var v = value
      var l = 0

      while (v != 0L) {
        if (l == 0) bufRaw(7 - l) = (v & 0x7f).toByte
        else bufRaw(7 - l) = ((v & 0x7f) | 0x80).toByte

        v = v >> 7
        l += 1
      }

      writeToStream(stream, bufRaw, 8 - l, l)
    } else {
      writeInt8(0)
    }
  }

  def writeBoolean(value: Boolean): Unit = value match {
    case true => stream.write(Array(1.toByte))
    case false => stream.write(Array(0.toByte))
  }

  def writeString(value: String): Unit = {
    val bytes = value.getBytes("UTF-8")
    writeInt7(bytes.length)
    writeToStream(stream, bytes, 0, bytes.length)
  }

  def writeBinary(value: Seq[Byte]): Unit = {
    writeInt7(value.length)
    writeToStream(stream, value.toArray, 0, value.length)
  }

  def writeBinary(value: Array[Byte]): Unit = {
    writeInt7(value.length)
    writeToStream(stream, value, 0, value.length)
  }

  def writeObjectId(value: ObjectId): Unit = {
    writeBinary(value.bytes.toArray)
  }

  def writeList[T](value: List[T])(inner: T => Any): Unit = {
    writeInt7(value.length)
    value.foreach(item => inner(item))
  }

  def close(): Unit = stream.close()

  private def writeToStream(stream: OutputStream, buffer: Array[Byte], offset: Int, length: Int) {
    stream.write(buffer, offset, length)
  }
}

class BinaryReader(val stream: InputStream) {
  private val bufRaw = new Array[Byte](8)
  private val buf = ByteBuffer.wrap(bufRaw)
  buf.order(ByteOrder.BIG_ENDIAN)

  def readInt8(): Byte = {
    readFromStream(stream, bufRaw, 0, 1)
    return buf.get(0)
  }

  def readInt16(): Short = {
    readFromStream(stream, bufRaw, 0, 2)
    return buf.getShort(0)
  }

  def readInt32(): Int = {
    readFromStream(stream, bufRaw, 0, 4)
    return buf.getInt(0)
  }

  def readInt64(): Long = {
    readFromStream(stream, bufRaw, 0, 8)
    return buf.getLong(0)
  }

  def readInt7(): Long = {
    var v = 0L
    var l = 0
    var done = false
    while (l < 8 && !done) {
      val b = readInt8()
      if (b < 0) {
        v = (v << 7) | (b & 0x7f)
      } else {
        v = (v << 7) | b
        done = true
      }
      l += 1
    }
    return v
  }

  def readBoolean(): Boolean = {
    return readInt8() != 0
  }

  def readString(): String = {
    val length = readInt7().toInt
    val stringBuf = new Array[Byte](length)
    readFromStream(stream, stringBuf, 0, length)
    return new String(stringBuf, "UTF-8")
  }

  def readBinary(): Array[Byte] = {
    val length = readInt7().toInt
    val buf = new Array[Byte](length)
    readFromStream(stream, buf, 0, length)
    return buf
  }

  def readObjectId(): ObjectId = {
    return ObjectId(readBinary)
  }

  def readList[T]()(inner: => T): List[T] = {
    (1 to readInt7().toInt).map(i => inner).toList
  }

  def close(): Unit = stream.close()

  @tailrec
  private def readFromStream(stream: InputStream, buffer: Array[Byte], offset: Int, length: Int) {
    if (length > 0) {
      val read = stream.read(buffer, offset, length)
      readFromStream(stream, buffer, offset + read, length - read)
    }
  }
}

object BinaryReaderWriter {
  implicit def inputStreamToBinaryReader(stream: InputStream) = new BinaryReader(stream)
  implicit def outputStreamToBinaryWriter(stream: OutputStream) = new BinaryWriter(stream)
}
