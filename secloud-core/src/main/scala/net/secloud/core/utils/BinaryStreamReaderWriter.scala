package net.secloud.core.utils

import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import net.secloud.core.objects.ObjectId

class BinaryStreamWriter(val stream: OutputStream) extends AnyVal with StreamWriter {
  def writeInt8(value: Byte): Unit = {
    val bufRaw = new Array[Byte](1)
    val buf = ByteBuffer.wrap(bufRaw)
    buf.order(ByteOrder.BIG_ENDIAN)

    buf.put(0, value)
    StreamUtils.writeBytes(stream, bufRaw, 0, 1)
  }

  def writeInt16(value: Short): Unit = {
    val bufRaw = new Array[Byte](2)
    val buf = ByteBuffer.wrap(bufRaw)
    buf.order(ByteOrder.BIG_ENDIAN)

    buf.putShort(0, value)
    StreamUtils.writeBytes(stream, bufRaw, 0, 2)
  }

  def writeInt32(value: Int): Unit = {
    val bufRaw = new Array[Byte](4)
    val buf = ByteBuffer.wrap(bufRaw)
    buf.order(ByteOrder.BIG_ENDIAN)

    buf.putInt(0, value)
    StreamUtils.writeBytes(stream, bufRaw, 0, 4)
  }

  def writeInt64(value: Long): Unit = {
    val bufRaw = new Array[Byte](8)
    val buf = ByteBuffer.wrap(bufRaw)
    buf.order(ByteOrder.BIG_ENDIAN)

    buf.putLong(0, value)
    StreamUtils.writeBytes(stream, bufRaw, 0, 8)
  }

  def writeInt7(value: Long): Unit = {
    assert(value >= 0L)
    assert(value < 72057594037927936L)

    val bufRaw = new Array[Byte](8)

    if (value > 0L) {
      var v = value
      var l = 0

      while (v != 0L) {
        if (l == 0) bufRaw(7 - l) = (v & 0x7f).toByte
        else bufRaw(7 - l) = ((v & 0x7f) | 0x80).toByte

        v = v >> 7
        l += 1
      }

      StreamUtils.writeBytes(stream, bufRaw, 8 - l, l)
    } else {
      writeInt8(0)
    }
  }

  def writeBoolean(value: Boolean): Unit = value match {
    case true ⇒ stream.write(Array(1.toByte))
    case false ⇒ stream.write(Array(0.toByte))
  }

  def writeString(value: String): Unit = {
    val bytes = value.getBytes("UTF-8")
    writeInt7(bytes.length)
    StreamUtils.writeBytes(stream, bytes, 0, bytes.length)
  }

  def writeBinary(value: Seq[Byte]): Unit = {
    writeInt7(value.length)
    StreamUtils.writeBytes(stream, value.toArray, 0, value.length)
  }

  def writeBinary(value: Array[Byte]): Unit = {
    writeInt7(value.length)
    StreamUtils.writeBytes(stream, value, 0, value.length)
  }

  def writeObjectId(value: ObjectId): Unit = {
    writeBinary(value.bytes.toArray)
  }

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

class BinaryStreamReader(val stream: InputStream) extends AnyVal with StreamReader {
  def readInt8(): Byte = {
    val bufRaw = new Array[Byte](1)
    val buf = ByteBuffer.wrap(bufRaw)
    buf.order(ByteOrder.BIG_ENDIAN)

    StreamUtils.readBytes(stream, bufRaw, 0, 1)
    return buf.get(0)
  }

  def readInt16(): Short = {
    val bufRaw = new Array[Byte](2)
    val buf = ByteBuffer.wrap(bufRaw)
    buf.order(ByteOrder.BIG_ENDIAN)

    StreamUtils.readBytes(stream, bufRaw, 0, 2)
    return buf.getShort(0)
  }

  def readInt32(): Int = {
    val bufRaw = new Array[Byte](4)
    val buf = ByteBuffer.wrap(bufRaw)
    buf.order(ByteOrder.BIG_ENDIAN)

    StreamUtils.readBytes(stream, bufRaw, 0, 4)
    return buf.getInt(0)
  }

  def readInt64(): Long = {
    val bufRaw = new Array[Byte](8)
    val buf = ByteBuffer.wrap(bufRaw)
    buf.order(ByteOrder.BIG_ENDIAN)

    StreamUtils.readBytes(stream, bufRaw, 0, 8)
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
    StreamUtils.readBytes(stream, stringBuf, 0, length)
    return new String(stringBuf, "UTF-8")
  }

  def readBinary(): Array[Byte] = {
    val length = readInt7().toInt
    val buf = new Array[Byte](length)
    StreamUtils.readBytes(stream, buf, 0, length)
    return buf
  }

  def readObjectId(): ObjectId = {
    return ObjectId(readBinary)
  }

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
