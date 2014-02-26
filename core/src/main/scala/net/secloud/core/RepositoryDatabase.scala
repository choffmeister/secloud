package net.secloud.core

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import net.secloud.core.objects._
import net.secloud.core.utils.StreamUtils._
import scala.annotation.tailrec

trait RepositoryDatabase {
  def init(): Unit

  def head: ObjectId
  def head_=(id: ObjectId): Unit

  def createReader(): ObjectReader
  def createWriter(): ObjectWriter
  def find(idPrefix: String): Option[ObjectId]

  def read[T](id: ObjectId)(inner: InputStream => T): T = {
    val reader = createReader()
    try {
      reader.open(id)
      inner(reader.stream)
    } finally {
      reader.close()
    }
  }

  def write(inner: OutputStream => ObjectId): ObjectId = {
    val writer = createWriter()
    try {
      writer.open()
      val id = inner(writer.stream)
      writer.close(id)
      id
    } finally {
      writer.close()
    }
  }
}

trait ObjectReader {
  def open(id: ObjectId): Unit
  def close(): Unit
  def stream: InputStream
}

trait ObjectWriter {
  def open(): Unit
  def close(id: ObjectId): Unit
  def close(): Unit
  def stream: OutputStream
}

class DirectoryRepositoryDatabase(val base: File) extends RepositoryDatabase {
  def this(base: String) = this(new File(base))

  def init() {
    if (base.exists()) throw new Exception(s"Cannot initialize database: Directory '$base' already exists")
    base.mkdirs()
  }

  def head: ObjectId = ObjectId(new String(readBytesFromFile(pathJoin(base, "HEAD")), "ASCII"))
  def head_=(id: ObjectId): Unit = writeBytesToFile(pathJoin(base, "HEAD"), id.hex.getBytes("ASCII"))

  def createReader(): ObjectReader = new DirectoryObjectReader(this)
  def createWriter(): ObjectWriter = new DirectoryObjectWriter(this)

  def find(idPrefix: String): Option[ObjectId] = {
    if (idPrefix.length >= 4) {
      val dir = pathJoin(base, List("objects", idPrefix.substring(0, 2)))
      if (dir.exists && dir.isDirectory) {
        val files = dir.listFiles().filter(f => f.getName.startsWith(idPrefix.substring(2))).toList
        if (files.length == 1) {
          Some(ObjectId(idPrefix.substring(0,2) + files(0).getName))
        } else None
      } else None
    } else None
  }

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

  class DirectoryObjectReader(val rdb: DirectoryRepositoryDatabase) extends ObjectReader {
    private var innerStream: Option[InputStream] = None

    def open(id: ObjectId) = innerStream match {
      case Some(s) => throw new Exception("Cannot open read stream twice")
      case _ => innerStream = Some(new BufferedInputStream(new FileInputStream(rdb.pathFromId(id)), 8192))
    }

    def close() = {
      if (innerStream.isDefined) {
        stream.close()
        innerStream = None
      }
    }

    def stream = innerStream match {
      case Some(s) => s
      case _ => throw new Exception("Read stream must be opened first")
    }
  }

  class DirectoryObjectWriter(val rdb: DirectoryRepositoryDatabase) extends ObjectWriter {
    private var innerStream: Option[OutputStream] = None
    private var tempPath: Option[File] = None

    def open() = innerStream match {
      case Some(s) => throw new Exception("Cannot open write stream twice")
      case _ =>
        tempPath = Some(rdb.createTempFile())
        innerStream = Some(new BufferedOutputStream(new FileOutputStream(tempPath.get), 8192))
    }

    def close(id: ObjectId) = {
      close()

      ensureDirectory(rdb.directoryFromId(id))

      if (!tempPath.get.renameTo(rdb.pathFromId(id))) {
        throw new Exception("Moving file to final position failed")
      }

      tempPath = None
    }

    def close() = {
      if (innerStream.isDefined) {
        stream.close()
        innerStream = None
      }
    }

    def stream = innerStream match {
      case Some(s) => s
      case _ => throw new Exception("Write stream must be opened first")
    }
  }
}
