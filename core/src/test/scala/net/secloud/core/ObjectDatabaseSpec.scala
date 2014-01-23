package net.secloud.core

import org.specs2.mutable._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import java.util.UUID
import java.io.File

@RunWith(classOf[JUnitRunner])
class ObjectDatabaseSpec extends Specification {
  def getTempDir = new File(new File(System.getProperty("java.io.tmpdir")), UUID.randomUUID().toString())

  "DirectoryObjectDatabase" should {
    "place written objects" in {
      val odb = new DirectoryObjectDatabase(getTempDir)
      val oid = ObjectId("00ff")

      val writer = odb.createWriter()
      writer.open()
      writer.stream.write("Hello World".getBytes("ASCII"))
      writer.close(oid)

      odb.pathFromId(oid).getAbsolutePath() must endWith("ff")
      odb.pathFromId(oid).exists() === true
    }

    "read objects" in {
      val odb = new DirectoryObjectDatabase(getTempDir)
      val oid = ObjectId("00ff")

      val writer = odb.createWriter()
      writer.open()
      writer.stream.write("Hello World".getBytes("ASCII"))
      writer.close(oid)

      val buffer = new Array[Byte](11)
      val reader = odb.createReader()
      reader.open(oid)
      reader.stream.read(buffer)

      new String(buffer, "ASCII") === "Hello World"
    }

    "find objects" in {
      val odb = new DirectoryObjectDatabase(getTempDir)
      val id1 = ObjectId("00112233445566")
      val id2 = ObjectId("0011223344eeff")
      val id3 = ObjectId("ffeeddccbbaa")

      val writer1 = odb.createWriter()
      writer1.open()
      writer1.stream.write("Hello World".getBytes("ASCII"))
      writer1.close(id1)

      val writer2 = odb.createWriter()
      writer2.open()
      writer2.stream.write("Hello World".getBytes("ASCII"))
      writer2.close(id2)

      val writer3 = odb.createWriter()
      writer3.open()
      writer3.stream.write("Hello World".getBytes("ASCII"))
      writer3.close(id3)

      odb.find("") === None

      odb.find("00") === None
      odb.find("0011") === None
      odb.find("001122") === None
      odb.find("00112233") === None
      odb.find("0011223344") === None
      odb.find("001122334455") === Some(id1)
      odb.find("00112233445566") === Some(id1)
      odb.find("0011223344ee") === Some(id2)
      odb.find("0011223344eeff") === Some(id2)

      odb.find("ff") === None
      odb.find("ffee") === Some(id3)
      odb.find("ffeedd") === Some(id3)
      odb.find("ffeeddcc") === Some(id3)
      odb.find("ffeeddccbb") === Some(id3)
      odb.find("ffeeddccbbaa") === Some(id3)
    }
  }
}
