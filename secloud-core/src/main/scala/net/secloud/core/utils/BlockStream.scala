package net.secloud.core.utils

import java.io.{ InputStream, OutputStream }
import scala.language.implicitConversions

private[utils] class BlockInputStream(val inner: InputStream, val ownsInner: Boolean = true) extends InputStream {
  private var block = Array.empty[Byte]
  private var blockSize = 0
  private var blockPosition = 0
  private var closed = false
  readBlock()

  override def read(): Int = {
    if (blockSize > 0) {
      val b = block(blockPosition) & 0xff // change range from [-128,127] to [0,255]
      blockPosition += 1
      if (blockSize > 0 && blockPosition == blockSize) readBlock()
      b
    } else -1
  }

  override def read(b: Array[Byte], off: Int, len: Int): Int = {
    if (blockSize > 0) {
      var totalReadCount = 0
      while (len - totalReadCount > 0) {
        val readCount = Math.min(blockSize - blockPosition, len - totalReadCount)
        if (readCount == 0) return totalReadCount;
        System.arraycopy(block, blockPosition, b, off + totalReadCount, readCount)
        blockPosition += readCount
        totalReadCount += readCount
        if (blockPosition == blockSize) readBlock()
      }
      totalReadCount
    } else -1
  }

  override def close(): Unit = {
    if (!closed) {
      if (ownsInner) inner.close()
      closed = true
    }
  }

  def skipToEnd(): Unit = {
    while (read() >= 0) {}
  }

  private def readBlock() {
    val size = inner.readInt7().toInt

    block = new Array[Byte](size)
    inner.read(block, 0, size)
    blockSize = size
    blockPosition = 0
  }
}

private[utils] class BlockOutputStream(val inner: OutputStream, val bufferSize: Int = 8192, val ownsInner: Boolean = true) extends OutputStream {
  private val block = new Array[Byte](bufferSize)
  private var blockPosition = 0
  private var closed = false

  override def write(b: Int): Unit = {
    if (blockPosition == bufferSize) flushBlock()

    block(blockPosition) = b.toByte
    blockPosition += 1
  }

  override def write(b: Array[Byte], off: Int, len: Int) = {
    var totalWriteCount = 0

    while (len - totalWriteCount > 0) {
      if (blockPosition == bufferSize) flushBlock()
      val writeCount = Math.min(bufferSize - blockPosition, len - totalWriteCount)
      System.arraycopy(b, off + totalWriteCount, block, blockPosition, writeCount)
      blockPosition += writeCount
      totalWriteCount += writeCount
    }
  }

  override def close(): Unit = {
    if (!closed) {
      // flush last block if non empty
      if (blockPosition > 0) flushBlock()
      // always end with an empty block
      flushBlock()

      if (ownsInner) inner.close()
      closed = true
    }
  }

  private def flushBlock() {
    inner.writeInt7(blockPosition)
    inner.write(block, 0, blockPosition)
    blockPosition = 0
  }
}
