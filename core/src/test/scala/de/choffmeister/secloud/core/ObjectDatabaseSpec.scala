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
  }
}
