package net.secloud.core.utils

import org.specs2.mutable._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import net.secloud.core.utils.BlockStream._
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@RunWith(classOf[JUnitRunner])
class BlockStreamSpec extends Specification {
  "RlockInputStream" should {
    "read properly" in {
      testRead(List[Byte](0)) === List[Byte]()
      testRead(List[Byte](1,0,1,1,1,2,1,3,0)) === List[Byte](0,1,2,3)
      testRead(List[Byte](2,0,1,2,2,3,0)) === List[Byte](0,1,2,3)
      testRead(List[Byte](3,0,1,2,1,3,0)) === List[Byte](0,1,2,3)
      testRead(List[Byte](4,0,1,2,3,0)) === List[Byte](0,1,2,3)

      testRead(List[Byte](1,0,1,1,1,-2,1,-1,0)) === List[Byte](0,1,-2,-1)
      testRead(List[Byte](2,0,1,2,-2,-1,0)) === List[Byte](0,1,-2,-1)
      testRead(List[Byte](3,0,1,-2,1,-1,0)) === List[Byte](0,1,-2,-1)
      testRead(List[Byte](4,0,1,-2,-1,0)) === List[Byte](0,1,-2,-1)
    }

    "jump to end of block even if content is unread" in {
      val b1 = List[Byte](1,0,1,1,1,2,1,3,0)
      val b2 = List[Byte](2,4,5,2,6,7,0)
      val readRaw = new ByteArrayInputStream((b1 ++ b2).toArray)

      val readBlock1 = new BlockInputStream(readRaw)
      readBlock1.read() === 0
      readBlock1.read() === 1
      readBlock1.skipToEnd()
      readBlock1.close()

      val readBlock2 = new BlockInputStream(readRaw)
      readBlock2.read() === 4
      readBlock2.read() === 5
      readBlock2.read() === 6
      readBlock2.read() === 7
      readBlock2.close()

      ok
    }
  }

  "BlockOutputStream" should {
    "write properly with different buffer sizes" in {
      testWrite(List[Byte](), 1) === List[Byte](0)
      testWrite(List[Byte](), 2) === List[Byte](0)
      testWrite(List[Byte](), 3) === List[Byte](0)
      testWrite(List[Byte](0,1,2,3), 1) === List[Byte](1,0,1,1,1,2,1,3,0)
      testWrite(List[Byte](0,1,2,3), 2) === List[Byte](2,0,1,2,2,3,0)
      testWrite(List[Byte](0,1,2,3), 3) === List[Byte](3,0,1,2,1,3,0)
      testWrite(List[Byte](0,1,2,3), 4) === List[Byte](4,0,1,2,3,0)
      testWrite(List[Byte](0,1,2,3), 5) === List[Byte](4,0,1,2,3,0)

      testWrite(List[Byte](0,1,-2,-1), 1) === List[Byte](1,0,1,1,1,-2,1,-1,0)
      testWrite(List[Byte](0,1,-2,-1), 2) === List[Byte](2,0,1,2,-2,-1,0)
      testWrite(List[Byte](0,1,-2,-1), 3) === List[Byte](3,0,1,-2,1,-1,0)
      testWrite(List[Byte](0,1,-2,-1), 4) === List[Byte](4,0,1,-2,-1,0)
      testWrite(List[Byte](0,1,-2,-1), 5) === List[Byte](4,0,1,-2,-1,0)
    }
  }

  def testRead(data: List[Byte]): List[Byte] = {
    val readRaw = new ByteArrayInputStream(data.toArray)
    val readBlock = new BlockInputStream(readRaw)

    var done = false
    var result = List.empty[Byte]
    while (!done) {
      val b = readBlock.read()
      if (b >= 0) {
        result = result ++ List[Byte](b.toByte)
      } else done = true
    }

    result
  }

  def testWrite(data: List[Byte], bufferSize: Int): List[Byte] = {
    val writeRaw = new ByteArrayOutputStream()
    val writeBlock = new BlockOutputStream(writeRaw, bufferSize)
    writeBlock.write(data.toArray)
    writeBlock.close()

    writeRaw.toByteArray.toList
  }
}
