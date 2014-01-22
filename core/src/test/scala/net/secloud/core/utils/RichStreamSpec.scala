package net.secloud.core.utils

import org.specs2.mutable._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import net.secloud.core.utils.RichStream._
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import org.apache.commons.codec.binary.Hex

@RunWith(classOf[JUnitRunner])
class RichStreamSpec extends Specification {
  "RichInputStream" should {
    "hash read data" in {
      val buf = "Hello World!".getBytes("ASCII")
      val readBuffer = new Array[Byte](128)
      val ms = new ByteArrayInputStream(buf)

      val actualHash = Hex.encodeHexString(ms.hashed("SHA-1") { hs =>
        hs.read(readBuffer) === 12
      })
      val expectedHash = "2ef7bde608ce5404e97d5f042f95f89f1c232871"

      actualHash === expectedHash
    }
  }

  "RichOutputStream" should {
    "cache output streams" in {
      val ms = new ByteArrayOutputStream()
      ms.write(0)
      ms.write(1)

      ms.cached(cs => ms.write(cs.size)) { cs =>
        cs.write(10)
        cs.write(11)
        cs.write(12)
        cs.write(13)

        ms.size === 2
      }

      ms.write(14)

      ms.toByteArray.toList === List[Byte](0, 1, 4, 10, 11, 12, 13, 14)
    }

    "hash written data" in {
      val ms = new ByteArrayOutputStream()

      val actualHash = Hex.encodeHexString(ms.hashed("SHA-1") { hs =>
        hs.write("Hello World!".getBytes("ASCII"))
      })
      val expectedHash = "2ef7bde608ce5404e97d5f042f95f89f1c232871"

      actualHash === expectedHash
    }
  }
}
