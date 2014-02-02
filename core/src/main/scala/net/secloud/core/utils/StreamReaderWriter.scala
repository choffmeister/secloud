package net.secloud.core.utils

import java.io._
import net.secloud.core.objects.ObjectId

trait StreamWriter {
  def writeInt8(value: Byte): Unit
  def writeInt16(value: Short): Unit
  def writeInt32(value: Int): Unit
  def writeInt64(value: Long): Unit
  def writeInt7(value: Long): Unit
  def writeBoolean(value: Boolean): Unit
  def writeString(value: String): Unit
  def writeBinary(value: Seq[Byte]): Unit
  def writeBinary(value: Array[Byte]): Unit
  def writeObjectId(value: ObjectId): Unit
  def writeList[T](value: List[T])(inner: T => Any): Unit
  def writeMap[A, B](value: Map[A, B])(inner: (A, B) => Any): Unit
  def close(): Unit
}

trait StreamReader {
  def readInt8(): Byte
  def readInt16(): Short
  def readInt32(): Int
  def readInt64(): Long
  def readInt7(): Long
  def readBoolean(): Boolean
  def readString(): String
  def readBinary(): Array[Byte]
  def readObjectId(): ObjectId
  def readList[T]()(inner: => T): List[T]
  def readMap[A, B]()(inner: => (A, B)): Map[A, B]
  def close(): Unit
}
