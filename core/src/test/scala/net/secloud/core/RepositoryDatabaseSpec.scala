package net.secloud.core

import org.specs2.mutable._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import java.util.UUID
import java.io.File
import net.secloud.core.objects._

@RunWith(classOf[JUnitRunner])
class RepositoryDatabaseSpec extends Specification {
  def getTempDir = new File(new File(System.getProperty("java.io.tmpdir")), UUID.randomUUID().toString())

  "DirectoryRepositoryDatabase" should {
    "place written objects" in {
      val rdb = new DirectoryRepositoryDatabase(getTempDir)
      val oid = ObjectId("00ff")

      val writer = rdb.createWriter()
      writer.open()
      writer.stream.write("Hello World".getBytes("ASCII"))
      writer.close(oid)

      rdb.pathFromId(oid).getAbsolutePath() must endWith("ff")
      rdb.pathFromId(oid).exists() === true
    }

    "read objects" in {
      val rdb = new DirectoryRepositoryDatabase(getTempDir)
      val oid = ObjectId("00ff")

      val writer = rdb.createWriter()
      writer.open()
      writer.stream.write("Hello World".getBytes("ASCII"))
      writer.close(oid)

      val buffer = new Array[Byte](11)
      val reader = rdb.createReader()
      reader.open(oid)
      reader.stream.read(buffer)

      new String(buffer, "ASCII") === "Hello World"
    }

    "find objects" in {
      val rdb = new DirectoryRepositoryDatabase(getTempDir)
      val id1 = ObjectId("00112233445566")
      val id2 = ObjectId("0011223344eeff")
      val id3 = ObjectId("ffeeddccbbaa")

      val writer1 = rdb.createWriter()
      writer1.open()
      writer1.stream.write("Hello World".getBytes("ASCII"))
      writer1.close(id1)

      val writer2 = rdb.createWriter()
      writer2.open()
      writer2.stream.write("Hello World".getBytes("ASCII"))
      writer2.close(id2)

      val writer3 = rdb.createWriter()
      writer3.open()
      writer3.stream.write("Hello World".getBytes("ASCII"))
      writer3.close(id3)

      rdb.find("") === None

      rdb.find("00") === None
      rdb.find("0011") === None
      rdb.find("001122") === None
      rdb.find("00112233") === None
      rdb.find("0011223344") === None
      rdb.find("001122334455") === Some(id1)
      rdb.find("00112233445566") === Some(id1)
      rdb.find("0011223344ee") === Some(id2)
      rdb.find("0011223344eeff") === Some(id2)

      rdb.find("ff") === None
      rdb.find("ffee") === Some(id3)
      rdb.find("ffeedd") === Some(id3)
      rdb.find("ffeeddcc") === Some(id3)
      rdb.find("ffeeddccbb") === Some(id3)
      rdb.find("ffeeddccbbaa") === Some(id3)
    }
  }
}
