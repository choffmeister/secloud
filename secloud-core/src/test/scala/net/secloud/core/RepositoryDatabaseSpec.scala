package net.secloud.core

import java.io.File
import java.util.UUID
import net.secloud.core.objects._
import org.specs2.mutable._

class RepositoryDatabaseSpec extends Specification {
  def getTempDir = new File(new File(System.getProperty("java.io.tmpdir")), UUID.randomUUID().toString())

  "DirectoryRepositoryDatabase" should {
    "persist objects" in {
      val rdb = new DirectoryRepositoryDatabase(getTempDir)
      val oid = ObjectId("00ff")

      val writer = rdb.createWriter()
      writer.open()
      writer.stream.write("Hello World".getBytes("ASCII"))
      writer.persist(oid)

      rdb.pathFromId(oid).getAbsolutePath() must endWith("ff")
      rdb.pathFromId(oid).exists() === true
    }

    "dismiss objects" in {
      val tempDir = getTempDir
      val rdb = new DirectoryRepositoryDatabase(tempDir)
      val oid = ObjectId("00ff")

      def ensureTempDirSize(n: Int) = {
        val files = Option(new File(tempDir, "temp").list()).getOrElse(Array.empty[String]).toSeq
        println(files)
        files must haveSize(n)
      }

      val writer = rdb.createWriter()
      ensureTempDirSize(0)
      writer.open()
      ensureTempDirSize(1)
      writer.stream.write("Hello World".getBytes("ASCII"))
      writer.dismiss()
      ensureTempDirSize(0)
    }

    "read objects" in {
      val rdb = new DirectoryRepositoryDatabase(getTempDir)
      val oid = ObjectId("00ff")

      val writer = rdb.createWriter()
      writer.open()
      writer.stream.write("Hello World".getBytes("ASCII"))
      writer.persist(oid)

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
      writer1.persist(id1)

      val writer2 = rdb.createWriter()
      writer2.open()
      writer2.stream.write("Hello World".getBytes("ASCII"))
      writer2.persist(id2)

      val writer3 = rdb.createWriter()
      writer3.open()
      writer3.stream.write("Hello World".getBytes("ASCII"))
      writer3.persist(id3)

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
