package net.secloud.core.utils

import java.io.{InputStream, OutputStream}
import net.secloud.core.utils._
import scala.language.implicitConversions

class BlockInputStream(val inner: InputStream, val ownsInner: Boolean = true) extends InputStream {
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

class BlockOutputStream(val inner: OutputStream, val bufferSize: Int = 8192, val ownsInner: Boolean = true) extends OutputStream {
  private val block = new Array[Byte](bufferSize)
  private var blockPosition = 0
  private var closed = false

  override def write(b: Int): Unit = {
    if (blockPosition == bufferSize) flushBlock()

    block(blockPosition) = b.toByte
    blockPosition += 1
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
