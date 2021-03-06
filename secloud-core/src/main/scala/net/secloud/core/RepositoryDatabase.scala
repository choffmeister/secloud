package net.secloud.core

import java.io._
import net.secloud.core.crypto._
import net.secloud.core.objects._
import net.secloud.core.utils.StreamUtils._
import scala.annotation.tailrec

trait RepositoryDatabase {
  def init(): Unit

  def headId: ObjectId
  def headId_=(id: ObjectId): Unit

  def createReader(): ObjectReader
  def createWriter(): ObjectWriter
  def find(idPrefix: String): Option[ObjectId]

  def read[T](id: ObjectId)(inner: InputStream ⇒ T): T = {
    val reader = createReader()
    try {
      reader.open(id)
      inner(reader.stream)
    } finally {
      reader.close()
    }
  }

  def write(inner: OutputStream ⇒ ObjectId): ObjectId = {
    writeExplicit { (stream, writer) ⇒
      val id = inner(stream)
      writer.persist(id)
      id
    }
  }

  def writeExplicit[T](inner: (OutputStream, ObjectWriter) ⇒ T): T = {
    val writer = createWriter()
    try {
      writer.open()
      inner(writer.stream, writer)
    } catch {
      case t: Throwable ⇒
        writer.dismiss()
        throw t
    } finally {
      writer.close()
    }
  }

  def writeCommit(commit: Commit, asymmetricKey: AsymmetricAlgorithmInstance, key: SymmetricAlgorithmInstance): ObjectId = {
    write { dbs ⇒
      ObjectSerializer.signObject(dbs, asymmetricKey) { ss ⇒
        ObjectSerializer.writeCommit(ss, commit, key)
      }
    }
  }
  def readCommit(commitId: ObjectId, key: Either[SymmetricAlgorithmInstance, AsymmetricAlgorithmInstance]): Commit =
    read(commitId)(s ⇒ ObjectSerializer.readCommit(s, key)).copy(id = commitId)

  def writeTree(tree: Tree, asymmetricKey: AsymmetricAlgorithmInstance, key: SymmetricAlgorithmInstance): ObjectId =
    write { dbs ⇒
      ObjectSerializer.signObject(dbs, asymmetricKey) { ss ⇒
        ObjectSerializer.writeTree(ss, tree, key)
      }
    }
  def readTree(treeId: ObjectId, key: SymmetricAlgorithmInstance): Tree =
    read(treeId)(s ⇒ ObjectSerializer.readTree(s, key)).copy(id = treeId)

  def writeBlobWithContent(blob: Blob, asymmetricKey: AsymmetricAlgorithmInstance, key: SymmetricAlgorithmInstance)(inner: OutputStream ⇒ Any): ObjectId =
    write { dbs ⇒
      ObjectSerializer.signObject(dbs, asymmetricKey) { ss ⇒
        ObjectSerializer.writeBlob(ss, blob)
        ObjectSerializer.writeBlobContent(ss, key)(inner)
      }
    }
  def readBlob(blobId: ObjectId): Blob =
    read(blobId)(s ⇒ ObjectSerializer.readBlob(s)).copy(id = blobId)
  def readBlobContent[T](blobId: ObjectId, key: SymmetricAlgorithmInstance)(inner: InputStream ⇒ T): T =
    read(blobId) { s ⇒
      ObjectSerializer.readBlob(s)
      ObjectSerializer.readBlobContent(s, key)(inner)
    }
}

trait ObjectReader {
  def open(id: ObjectId): Unit
  def close(): Unit
  def stream: InputStream
}

trait ObjectWriter {
  def open(): Unit
  def persist(id: ObjectId): Unit
  def dismiss(): Unit
  def close(): Unit
  def stream: OutputStream
}

class DirectoryRepositoryDatabase(val base: File) extends RepositoryDatabase {
  private val log = org.slf4j.LoggerFactory.getLogger(getClass)

  def this(base: String) = this(new File(base))

  def init() {
    log.info(s"Initializing a new repository at $base")
    if (base.exists()) throw new Exception(s"Cannot initialize database: Directory '$base' already exists")
    base.mkdirs()
  }

  def headId: ObjectId = ObjectId(new String(readBytesFromFile(pathJoin(base, "HEAD")), "ASCII"))
  def headId_=(id: ObjectId): Unit = writeBytesToFile(pathJoin(base, "HEAD"), id.hex.getBytes("ASCII"))

  def createReader(): ObjectReader = new DirectoryObjectReader(this)
  def createWriter(): ObjectWriter = new DirectoryObjectWriter(this)

  def find(idPrefix: String): Option[ObjectId] = {
    if (idPrefix.length >= 4) {
      val dir = pathJoin(base, List("objects", idPrefix.substring(0, 2)))
      if (dir.exists && dir.isDirectory) {
        val files = dir.listFiles().filter(f ⇒ f.getName.startsWith(idPrefix.substring(2))).toList
        if (files.length == 1) {
          Some(ObjectId(idPrefix.substring(0, 2) + files(0).getName))
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
    case first :: rest ⇒ pathJoin(new File(base, first), rest)
    case Nil ⇒ base
  }

  class DirectoryObjectReader(val rdb: DirectoryRepositoryDatabase) extends ObjectReader {
    private var innerStream: Option[InputStream] = None

    def open(id: ObjectId) = innerStream match {
      case Some(s) ⇒ throw new Exception("Cannot open read stream twice")
      case _ ⇒ innerStream = Some(new BufferedInputStream(new FileInputStream(rdb.pathFromId(id)), 8192))
    }

    def close() = {
      if (innerStream.isDefined) {
        stream.close()
        innerStream = None
      }
    }

    def stream = innerStream match {
      case Some(s) ⇒ s
      case _ ⇒ throw new Exception("Read stream must be opened first")
    }
  }

  class DirectoryObjectWriter(val rdb: DirectoryRepositoryDatabase) extends ObjectWriter {
    private var innerStream: Option[OutputStream] = None
    private var tempPath: Option[File] = None

    def open() = innerStream match {
      case Some(s) ⇒ throw new Exception("Cannot open write stream twice")
      case _ ⇒
        tempPath = Some(rdb.createTempFile())
        innerStream = Some(new BufferedOutputStream(new FileOutputStream(tempPath.get), 8192))
    }

    def persist(id: ObjectId) = {
      close()

      ensureDirectory(rdb.directoryFromId(id))

      val finalPath = rdb.pathFromId(id)
      if (!tempPath.get.renameTo(finalPath)) {
        throw new Exception(s"Moving temporary file $tempPath to final position $finalPath failed")
      }

      tempPath = None
    }

    def dismiss() = {
      close()

      tempPath.get.delete()
    }

    def close() = {
      if (innerStream.isDefined) {
        stream.close()
        innerStream = None
      }
    }

    def stream = innerStream match {
      case Some(s) ⇒ s
      case _ ⇒ throw new Exception("Write stream must be opened first")
    }
  }
}
