package de.choffmeister.secloud.core.utils

import java.io.OutputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class BinaryWriter(val stream: OutputStream) {
  private val bufRaw = new Array[Byte](8)
  private val buf = ByteBuffer.wrap(bufRaw)
  buf.order(ByteOrder.BIG_ENDIAN)

  def writeInt8(value: Byte): Unit = {
    buf.put(0, value)
    stream.write(bufRaw, 0, 1)
  }

  def writeInt16(value: Short): Unit = {
    buf.putShort(0, value)
    stream.write(bufRaw, 0, 2)
  }

  def writeInt32(value: Int): Unit = {
    buf.putInt(0, value)
    stream.write(bufRaw, 0, 4)
  }

  def writeInt64(value: Long): Unit = {
    buf.putLong(0, value)
    stream.write(bufRaw, 0, 8)
  }

  def writeBoolean(value: Boolean): Unit = value match {
    case true => stream.write(Array(1.toByte))
    case false => stream.write(Array(0.toByte))
  }

  def writeString(value: String): Unit = {
    val bytes = value.getBytes("UTF-8")
    writeInt32(bytes.length)
    stream.write(bytes)
  }

  def writeBinary(value: Array[Byte]): Unit = {
    writeInt32(value.length)
    stream.write(value)
  }

  def close(): Unit = stream.close()
}

class BinaryReader(val stream: InputStream) {
  private val bufRaw = new Array[Byte](8)
  private val buf = ByteBuffer.wrap(bufRaw)
  buf.order(ByteOrder.BIG_ENDIAN)

  def readInt8(): Byte = {
    stream.read(bufRaw, 0, 1)
    return buf.get(0)
  }

  def readInt16(): Short = {
    stream.read(bufRaw, 0, 2)
    return buf.getShort(0)
  }

  def readInt32(): Int = {
    stream.read(bufRaw, 0, 4)
    return buf.getInt(0)
  }

  def readInt64(): Long = {
    stream.read(bufRaw, 0, 8)
    return buf.getLong(0)
  }

  def readBoolean(): Boolean = {
    return readInt8() != 0
  }

  def readString(): String = {
    val length = readInt32()
    val stringBuf = new Array[Byte](length)
    stream.read(stringBuf, 0, length)
    return new String(stringBuf, "UTF-8")
  }

  def readBinary(): Array[Byte] = {
    val length = readInt32()
    val buf = new Array[Byte](length)
    stream.read(buf, 0, length)
    return buf
  }

  def close(): Unit = stream.close()
}