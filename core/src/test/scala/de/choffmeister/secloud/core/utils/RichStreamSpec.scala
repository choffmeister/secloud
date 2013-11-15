package de.choffmeister.secloud.core.utils

import org.specs2.mutable._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import de.choffmeister.secloud.core.utils.RichStream._
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException

@RunWith(classOf[JUnitRunner])
class RichStreamSpec extends Specification {
  "RichInputStream" should {
    "cap inner streams" in {
      val buf = Array[Byte](0, 1, 2, 3, 4)
      val ms = new ByteArrayInputStream(buf)
      ms.read() === 0.toByte
      ms.read() === 1.toByte

      ms.preSizedInner(2) { s =>
        s.read() === 2.toByte
        s.read() === 3.toByte
        s.read()
      } must throwA[IOException]

      ms.read() === 4.toByte
    }

    "ensure puter stream position" in {
      val buf = Array[Byte](0, 1, 2, 3, 4)
      val ms = new ByteArrayInputStream(buf)
      ms.read() === 0.toByte
      ms.read() === 1.toByte

      ms.preSizedInner(2) { s =>
        s.read() === 2.toByte
      }

      ms.read() === 4.toByte
    }
  }

  "RichOutputStream" should {
    "cap inner streams" in {
      val ms = new ByteArrayOutputStream()
      ms.write(0)
      ms.write(1)

      ms.preSizedInner(3) { s =>
        s.write(2)
        s.write(3)
        s.write(4)
        s.write(5)
      } must throwA[IOException]

      ms.write(6)

      ms.toByteArray.toList === List[Byte](0, 1, 2, 3, 4, 6)
    }

    "ensure puter stream position" in {
      val ms = new ByteArrayOutputStream()
      ms.write(0)
      ms.write(1)

      ms.preSizedInner(3) { s =>
        s.write(2)
        s.write(3)
      }

      ms.toByteArray.toList === List[Byte](0, 1, 2, 3, 0)
    }
  }
}