package de.choffmeister.secloud.core.utils

import org.specs2.mutable._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import de.choffmeister.secloud.core.ObjectId

@RunWith(classOf[JUnitRunner])
class BinaryReaderWriterSpec extends Specification {
  "BinaryReaderWriter" should {
    "use big endian (network byte order)" in {
      val streamWrite = new ByteArrayOutputStream()
      val writer = new BinaryWriter(streamWrite)

      writer.writeInt8(1)
      writer.writeInt16(16385)
      writer.writeInt32(134480385)
      writer.writeInt64(9169364094552375809L)
      
      val buf = streamWrite.toByteArray()
      buf.length === 15
      
      buf(0) === 1.toByte
      
      buf(1) === 64.toByte
      buf(2) === 1.toByte
      
      buf(3) === 8.toByte
      buf(4) === 4.toByte
      buf(5) === 2.toByte
      buf(6) === 1.toByte
      
      buf(7) === 127.toByte
      buf(8) === 64.toByte
      buf(9) === 32.toByte
      buf(10) === 16.toByte
      buf(11) === 8.toByte
      buf(12) === 4.toByte
      buf(13) === 2.toByte
      buf(14) === 1.toByte
    }

    "read and write Int8" in {
      val streamWrite = new ByteArrayOutputStream()
      val writer = new BinaryWriter(streamWrite)

      writer.writeInt8(Byte.MinValue)
      writer.writeInt8(1.toByte)
      writer.writeInt8(Byte.MaxValue)
      writer.close()

      val buf = streamWrite.toByteArray()
      buf.length === 3

      val streamRead = new ByteArrayInputStream(buf)
      val reader = new BinaryReader(streamRead)

      reader.readInt8() === Byte.MinValue
      reader.readInt8() === 1.toByte
      reader.readInt8() === Byte.MaxValue
      reader.close()
      ok
    }

    "read and write Int16" in {
      val streamWrite = new ByteArrayOutputStream()
      val writer = new BinaryWriter(streamWrite)

      writer.writeInt16(Short.MinValue)
      writer.writeInt16(16385.toShort)
      writer.writeInt16(Short.MaxValue)
      writer.close()

      val buf = streamWrite.toByteArray()
      buf.length === 6

      val streamRead = new ByteArrayInputStream(buf)
      val reader = new BinaryReader(streamRead)

      reader.readInt16() === Short.MinValue
      reader.readInt16() === 16385.toShort
      reader.readInt16() === Short.MaxValue
      reader.close()
      ok
    }

    "read and write Int32" in {
      val streamWrite = new ByteArrayOutputStream()
      val writer = new BinaryWriter(streamWrite)

      writer.writeInt32(Int.MinValue)
      writer.writeInt32(134480385)
      writer.writeInt32(Int.MaxValue)
      writer.close()

      val buf = streamWrite.toByteArray()
      buf.length === 12

      val streamRead = new ByteArrayInputStream(buf)
      val reader = new BinaryReader(streamRead)

      reader.readInt32() === Int.MinValue
      reader.readInt32() === 134480385
      reader.readInt32() === Int.MaxValue
      reader.close()
      ok
    }

    "read and write Int64" in {
      val streamWrite = new ByteArrayOutputStream()
      val writer = new BinaryWriter(streamWrite)

      writer.writeInt64(Long.MinValue)
      writer.writeInt64(9169364094552375809L)
      writer.writeInt64(Long.MaxValue)
      writer.close()

      val buf = streamWrite.toByteArray()
      buf.length === 24

      val streamRead = new ByteArrayInputStream(buf)
      val reader = new BinaryReader(streamRead)

      reader.readInt64() === Long.MinValue
      reader.readInt64() === 9169364094552375809L
      reader.readInt64() === Long.MaxValue
      reader.close()
      ok
    }

    "read and write Int7" in {
      val streamWrite = new ByteArrayOutputStream()
      val writer = new BinaryWriter(streamWrite)

      writer.writeInt7(0L)
      streamWrite.size === 1
      writer.writeInt7((1L << 7L) - 1L)
      streamWrite.size === 2
      writer.writeInt7((1L << 7L))
      streamWrite.size === 4
      writer.writeInt7((1L << 14L) - 1L)
      streamWrite.size === 6
      writer.writeInt7((1L << 14L))
      streamWrite.size === 9
      writer.writeInt7((1L << 21L) - 1L)
      streamWrite.size === 12
      writer.writeInt7((1L << 21L))
      streamWrite.size === 16
      writer.writeInt7((1L << 28L) - 1L)
      streamWrite.size === 20
      writer.writeInt7((1L << 28L))
      streamWrite.size === 25
      writer.writeInt7((1L << 35L) - 1L)
      streamWrite.size === 30
      writer.writeInt7((1L << 35L))
      streamWrite.size === 36
      writer.writeInt7((1L << 42L) - 1L)
      streamWrite.size === 42
      writer.writeInt7((1L << 42L))
      streamWrite.size === 49
      writer.writeInt7((1L << 49L) - 1L)
      streamWrite.size === 56
      writer.writeInt7((1L << 49L))
      streamWrite.size === 64
      writer.writeInt7((1L << 56L) - 1L)
      streamWrite.size === 72
      writer.close()

      val buf = streamWrite.toByteArray()
      buf.length === 72

      val streamRead = new ByteArrayInputStream(buf)
      val reader = new BinaryReader(streamRead)

      reader.readInt7() === 0L
      reader.readInt7() === 127L
      reader.readInt7() === 128L
      reader.readInt7() === 16383L
      reader.readInt7() === 16384L
      reader.readInt7() === 2097151L
      reader.readInt7() === 2097152L
      reader.readInt7() === 268435455L
      reader.readInt7() === 268435456L
      reader.readInt7() === 34359738367L
      reader.readInt7() === 34359738368L
      reader.readInt7() === 4398046511103L
      reader.readInt7() === 4398046511104L
      reader.readInt7() === 562949953421311L
      reader.readInt7() === 562949953421312L
      reader.readInt7() === 72057594037927935L
      reader.close()
      ok
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
      buf.length === 33

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
      buf.length === 29

      val streamRead = new ByteArrayInputStream(buf)
      val reader = new BinaryReader(streamRead)

      reader.readBinary() === Array(Byte.MinValue, Byte.MaxValue)
      reader.readBinary() === Array.empty[Byte]
      reader.readBinary() === Array(10.toByte, 11.toByte, 12.toByte)
    }

    "read and write ObjectId" in {
      val streamWrite = new ByteArrayOutputStream()
      val writer = new BinaryWriter(streamWrite)

      writer.writeObjectId(ObjectId("000000"))
      writer.writeObjectId(ObjectId())
      writer.writeObjectId(ObjectId("ffffffffffffffffffffffff"))
      writer.close()

      val buf = streamWrite.toByteArray()
      buf.length === 39

      val streamRead = new ByteArrayInputStream(buf)
      val reader = new BinaryReader(streamRead)

      reader.readObjectId() === ObjectId("000000")
      reader.readObjectId() === ObjectId()
      reader.readObjectId() === ObjectId("ffffffffffffffffffffffff")
    }
  }
}