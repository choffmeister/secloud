package net.secloud.core

import java.io.File

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
  def list(element: WorkingDirElement): List[WorkingDirElement]
  def list(path: String): List[WorkingDirElement] = list(toElement(path))

  def toElement(path: String): WorkingDirElement
}

class DirectoryRepositoryWorkingDir(val base: File) extends RepositoryWorkingDir {
  def this(base: String) = this(new File(base))

  def list(element: WorkingDirElement): List[WorkingDirElement] = {
    toFile(element).listFiles.toList
      .map(f => element.path + "/" + f.getName)
      .map(p => toElement(p))
      .sortBy(e => e.name)
      .toList
  }

  def toElement(path: String) = {
    val unknown = WorkingDirElement(path)
    val file = toFile(unknown)
    val mode = file.isDirectory match {
      case true => DirectoryElementMode
      // TODO: check if file is executable
      case false => NonExecutableFileElementMode
    }
    unknown.copy(mode = mode)
  }

  private def toFile(element: WorkingDirElement): File =
    new File(base, element.segments.mkString(File.separator))
}
