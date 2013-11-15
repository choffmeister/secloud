package de.choffmeister.secloud.core.utils

import org.specs2.mutable._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

@RunWith(classOf[JUnitRunner])
class BinaryReaderWriterSpec extends Specification {
  "BinaryReaderWriter" should {
    "read and write Int8" in {
      val streamWrite = new ByteArrayOutputStream()
      val writer = new BinaryWriter(streamWrite)

      writer.writeInt8(Byte.MinValue)
      writer.writeInt8(Byte.MaxValue)
      writer.close()

      val buf = streamWrite.toByteArray()
      buf.length === 2

      val streamRead = new ByteArrayInputStream(buf)
      val reader = new BinaryReader(streamRead)

      reader.readInt8() === Byte.MinValue
      reader.readInt8() === Byte.MaxValue
    }

    "read and write Int16" in {
      val streamWrite = new ByteArrayOutputStream()
      val writer = new BinaryWriter(streamWrite)

      writer.writeInt16(Short.MinValue)
      writer.writeInt16(Short.MaxValue)
      writer.close()

      val buf = streamWrite.toByteArray()
      buf.length === 4

      val streamRead = new ByteArrayInputStream(buf)
      val reader = new BinaryReader(streamRead)

      reader.readInt16() === Short.MinValue
      reader.readInt16() === Short.MaxValue
    }

    "read and write Int32" in {
      val streamWrite = new ByteArrayOutputStream()
      val writer = new BinaryWriter(streamWrite)

      writer.writeInt32(Int.MinValue)
      writer.writeInt32(Int.MaxValue)
      writer.close()

      val buf = streamWrite.toByteArray()
      buf.length === 8

      val streamRead = new ByteArrayInputStream(buf)
      val reader = new BinaryReader(streamRead)

      reader.readInt32() === Int.MinValue
      reader.readInt32() === Int.MaxValue
    }

    "read and write Int64" in {
      val streamWrite = new ByteArrayOutputStream()
      val writer = new BinaryWriter(streamWrite)

      writer.writeInt64(Long.MinValue)
      writer.writeInt64(Long.MaxValue)
      writer.close()

      val buf = streamWrite.toByteArray()
      buf.length === 16

      val streamRead = new ByteArrayInputStream(buf)
      val reader = new BinaryReader(streamRead)

      reader.readInt64() === Long.MinValue
      reader.readInt64() === Long.MaxValue
    }

    "read and write Boolean" in {
      val streamWrite = new ByteArrayOutputStream()
      val writer = new BinaryWriter(streamWrite)

      writer.writeBoolean(true)
      writer.writeBoolean(false)
      writer.close()

      val buf = streamWrite.toByteArray()
      buf.length === 2

      val streamRead = new ByteArrayInputStream(buf)
      val reader = new BinaryReader(streamRead)

      reader.readBoolean() === true
      reader.readBoolean() === false
    }

    "read and write String" in {
      val streamWrite = new ByteArrayOutputStream()
      val writer = new BinaryWriter(streamWrite)

      writer.writeString("foo")
      writer.writeString("")
      writer.writeString("äöü")
      writer.close()

      val buf = streamWrite.toByteArray()
      buf.length === 21

      val streamRead = new ByteArrayInputStream(buf)
      val reader = new BinaryReader(streamRead)

      reader.readString() === "foo"
      reader.readString() === ""
      reader.readString() === "äöü"
    }

    "read and write Binary" in {
      val streamWrite = new ByteArrayOutputStream()
      val writer = new BinaryWriter(streamWrite)

      writer.writeBinary(Array(Byte.MinValue, Byte.MaxValue))
      writer.writeBinary(Array.empty[Byte])
      writer.writeBinary(Array(10.toByte, 11.toByte, 12.toByte))
      writer.close()

      val buf = streamWrite.toByteArray()
      buf.length === 17

      val streamRead = new ByteArrayInputStream(buf)
      val reader = new BinaryReader(streamRead)

      reader.readBinary() === Array(Byte.MinValue, Byte.MaxValue)
      reader.readBinary() === Array.empty[Byte]
      reader.readBinary() === Array(10.toByte, 11.toByte, 12.toByte)
    }
  }
}