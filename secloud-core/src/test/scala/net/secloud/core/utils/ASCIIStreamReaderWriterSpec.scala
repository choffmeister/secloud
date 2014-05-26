package net.secloud.core.utils

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import net.secloud.core.objects.ObjectId
import org.specs2.mutable._

class ASCIIStreamReaderWriterSpec extends Specification {
  "ASCIIStreamReaderWriter" should {
    "read and write Binary" in {
      val streamWrite = new ByteArrayOutputStream()
      val writer = new ASCIIStreamWriter(streamWrite)

      writer.writeBinary("abc".getBytes())
      writer.writeBinary("123".getBytes())
      writer.close()

      val buf = streamWrite.toByteArray()

      new String(buf, "ASCII") === "3:abc\n3:123\n"

      val streamRead = new ByteArrayInputStream(buf)
      val reader = new ASCIIStreamReader(streamRead)

      reader.readBinary() === "abc".getBytes()
      reader.readBinary() === "123".getBytes()
    }
  }
}
