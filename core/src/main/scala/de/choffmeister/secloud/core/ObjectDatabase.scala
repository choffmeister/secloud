package net.secloud.core

import java.io.File
import scala.annotation.tailrec
import java.io.InputStream
import java.io.OutputStream
import java.io.FileInputStream
import java.io.FileOutputStream

trait ObjectDatabase {
  def createReader(): ObjectReader
  def createWriter(): ObjectWriter
}

trait ObjectReader {
  def open(id: ObjectId): Unit
  def close(): Unit
  def stream: InputStream
}

trait ObjectWriter {
  def open(): Unit
  def close(id: ObjectId): Unit
  def stream: OutputStream
}

class DirectoryObjectDatabase(val base: File) extends ObjectDatabase {
  def this(base: String) = this(new File(base))

  ensureDirectory(base)

  def createReader(): ObjectReader = new DirectoryObjectReader(this)

  def createWriter(): ObjectWriter = new DirectoryObjectWriter(this)

  def directoryFromId(id: ObjectId) = pathJoin(base, List("objects", id.hex.substring(0, 2)))

  def pathFromId(id: ObjectId) = pathJoin(directoryFromId(id), id.hex.substring(2))

  private def ensureDirectory(path: File): Unit = if (path.exists()) {
    if (!path.isDirectory()) {
      throw new Exception(s"Path '$path' is not a directory")
    }
  } else path.mkdirs()

  private def createTempFile(): File = {
    val tempDirectory = pathJoin(base, "temp")
    ensureDirectory(tempDirectory)

    File.createTempFile("writer-", "", tempDirectory)
  }

  private def pathJoin(base: File, segment: String): File = pathJoin(base, List(segment))

  @tailrec
  private def pathJoin(base: File, segments: List[String]): File = segments match {
    case first :: rest => pathJoin(new File(base, first), rest)
    case Nil => base
  }

  class DirectoryObjectReader(val odb: DirectoryObjectDatabase) extends ObjectReader {
    private var innerStream: Option[InputStream] = None

    def open(id: ObjectId) = innerStream match {
      case Some(s) => throw new Exception("Cannot open read stream twice")
      case _ => innerStream = Some(new FileInputStream(odb.pathFromId(id)))
    }

    def close() = {
      stream.close()
      innerStream = None
    }

    def stream = innerStream match {
      case Some(s) => s
      case _ => throw new Exception("Read stream must be opened first")
    }
  }

  class DirectoryObjectWriter(val odb: DirectoryObjectDatabase) extends ObjectWriter {
    private var innerStream: Option[OutputStream] = None
    private var tempPath: Option[File] = None

    def open() = innerStream match {
      case Some(s) => throw new Exception("Cannot open write stream twice")
      case _ =>
        tempPath = Some(odb.createTempFile())
        innerStream = Some(new FileOutputStream(tempPath.get))
    }

    def close(id: ObjectId) = {
      stream.close()

      ensureDirectory(odb.directoryFromId(id))

      if (!tempPath.get.renameTo(odb.pathFromId(id))) {
        throw new Exception("Moving file to final position failed")
      }

      tempPath = None
      innerStream = None
    }

    def stream = innerStream match {
      case Some(s) => s
      case _ => throw new Exception("Write stream must be opened first")
    }
  }
}
