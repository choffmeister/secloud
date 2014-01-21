package net.secloud.core.utils

import java.io.{InputStream, OutputStream}
import net.secloud.core.utils.BinaryReaderWriter._
import scala.language.implicitConversions

class BlockInputStream(val inner: InputStream, val ownsInner: Boolean = true) extends InputStream {
  private var startedReading = false
  private var block = Array.empty[Byte]
  private var blockSize = 0
  private var blockPosition = 0

  override def read(): Int = {
    if (!startedReading || (blockSize > 0 && blockPosition == blockSize)) readBlock()

    if (blockSize > 0) {
      val b = block(blockPosition)
      blockPosition += 1
      b
    } else -1
  }

  override def close(): Unit = {
    // consume the whole remaining block stream
    while (read() >= 0) {}

    if (ownsInner) inner.close()
  }

  private def readBlock() {
    val size = inner.readInt7().toInt

    block = new Array[Byte](size)
    inner.read(block, 0, size)
    blockSize = size
    blockPosition = 0
    startedReading = true
  }
}

class BlockOutputStream(val inner: OutputStream, val bufferSize: Int = 8192, val ownsInner: Boolean = true) extends OutputStream {
  private val block = new Array[Byte](bufferSize)
  private var blockPosition = 0

  override def write(b: Int): Unit = {
    if (blockPosition == bufferSize) flushBlock()

    block(blockPosition) = b.toByte
    blockPosition += 1
  }

  override def close(): Unit = {
    // flush last block if non empty
    if (blockPosition > 0) flushBlock()
    // always end with an empty block
    flushBlock()

    if (ownsInner) inner.close()
  }

  private def flushBlock() {
    inner.writeInt7(blockPosition)
    inner.write(block, 0, blockPosition)
    blockPosition = 0
  }
}

object BlockStream {
  implicit def inputStreamToBlockInputStream(stream: InputStream) = new BlockInputStream(stream)
  implicit def outputStreamToBlockOutputStream(stream: OutputStream) = new BlockOutputStream(stream)
}
