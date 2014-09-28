package net.secloud.core.backend

import java.io._

import net.secloud.core.objects.ObjectId

trait Backend {
  def init(): Unit
  def wipe(): Unit

  def headId: ObjectId
  def headId_=(id: ObjectId): Unit

  def has(id: ObjectId): Boolean
  def put(id: ObjectId, input: InputStream): Unit
  def get(id: ObjectId, output: OutputStream): Unit

  def putBytes(id: ObjectId, bytes: Array[Byte]): Unit = {
    put(id, new ByteArrayInputStream(bytes))
  }

  def getBytes(id: ObjectId): Array[Byte] = {
    val stream = new ByteArrayOutputStream()
    get(id, stream)
    stream.toByteArray
  }
}
