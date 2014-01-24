package net.secloud.core

import java.io.{InputStream, OutputStream}
import java.io.{BufferedInputStream, BufferedOutputStream}
import java.io.{File, FileInputStream, FileOutputStream}

sealed abstract class WorkingDirElementMode
case object UnknownElementMode extends WorkingDirElementMode
case object NonExecutableFileElementMode extends WorkingDirElementMode
case object ExecutableFileElementMode extends WorkingDirElementMode
case object DirectoryElementMode extends WorkingDirElementMode

case class WorkingDirElement(
  path: String,
  segments: List[String],
  name: String,
  mode: WorkingDirElementMode
)

object WorkingDirElement {
  def apply(path: String): WorkingDirElement = {
    val segments = path.split("/").filter(_ != "").toList
    val name = segments.lastOption match {
      case Some(name) => name
      case _ => ""
    }

    new WorkingDirElement("/" + segments.mkString("/"), segments, name, UnknownElementMode)
  }
}

trait RepositoryWorkingDir {
  def init(): Unit

  def createReader(): WorkingDirElementReader
  def createWriter(): WorkingDirElementWriter

  def read[T](element: WorkingDirElement)(inner: InputStream => T): T = {
    val reader = createReader()
    try {
      reader.open(element)
      inner(reader.stream)
    } finally {
      reader.close()
    }
  }

  def write(element: WorkingDirElement)(inner: OutputStream => Any): Unit = {
    val writer = createWriter()
    try {
      writer.open(element)
      inner(writer.stream)
    } finally {
      writer.close()
    }
  }

  def list(element: WorkingDirElement): List[WorkingDirElement]
  def pathToElement(path: String): WorkingDirElement

  def read[T](path: String)(inner: InputStream => T): T = read(pathToElement(path))(inner)
  def write(path: String)(inner: OutputStream => Any): Unit = write(pathToElement(path))(inner)
  def list(path: String): List[WorkingDirElement] = list(pathToElement(path))
}

trait WorkingDirElementReader {
  def open(element: WorkingDirElement): Unit
  def close(): Unit
  def stream: InputStream
}

trait WorkingDirElementWriter {
  def open(element: WorkingDirElement): Unit
  def close(): Unit
  def stream: OutputStream
}

class DirectoryRepositoryWorkingDir(val base: File) extends RepositoryWorkingDir {
  def this(base: String) = this(new File(base))

  def init(): Unit = base.mkdirs()

  def createReader(): WorkingDirElementReader = new DirectoryWorkingDirElementReader(this)
  def createWriter(): WorkingDirElementWriter = new DirectoryWorkingDirElementWriter(this)

  def list(element: WorkingDirElement): List[WorkingDirElement] = {
    elementToFile(element).listFiles.toList
      .map(f => element.path + "/" + f.getName)
      .map(p => pathToElement(p))
      .sortBy(e => e.name)
      .toList
  }

  def pathToElement(path: String) = {
    val unknown = WorkingDirElement(path)
    val file = elementToFile(unknown)
    val mode = file.isDirectory match {
      case true => DirectoryElementMode
      // TODO: check if file is executable
      case false => NonExecutableFileElementMode
    }
    unknown.copy(mode = mode)
  }

  private def elementToFile(element: WorkingDirElement): File =
    new File(base, element.segments.mkString(File.separator))

  class DirectoryWorkingDirElementReader(val rwd: DirectoryRepositoryWorkingDir) extends WorkingDirElementReader {
    private var innerStream: Option[InputStream] = None

    def open(element: WorkingDirElement) = innerStream match {
      case Some(s) => throw new Exception("Cannot open read stream twice")
      case _ => innerStream = Some(new BufferedInputStream(new FileInputStream(rwd.elementToFile(element)), 8192))
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

  class DirectoryWorkingDirElementWriter(val rwd: DirectoryRepositoryWorkingDir) extends WorkingDirElementWriter {
    private var innerStream: Option[OutputStream] = None

    def open(element: WorkingDirElement) = innerStream match {
      case Some(s) => throw new Exception("Cannot open write stream twice")
      case _ => innerStream = Some(new BufferedOutputStream(new FileOutputStream(rwd.elementToFile(element)), 8192))
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
