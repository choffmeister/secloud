package net.secloud.core

import java.io.{InputStream, OutputStream}
import java.io.{File, FileInputStream, FileOutputStream}
import scala.language.reflectiveCalls
import scala.language.implicitConversions

sealed abstract class VirtualFileMode
case object NonExecutableFile extends VirtualFileMode
case object ExecutableFile extends VirtualFileMode
case object Directory extends VirtualFileMode

case class VirtualFile(vfs: VirtualFileSystem, path: String) {
  VirtualFile.checkPath(path)
  def segments = VirtualFile.splitPath(path)

  def exists: Boolean = vfs.exists(this)
  def mode: VirtualFileMode = vfs.mode(this)
}

object VirtualFile {
  def normalize(path: String): String =
    "/" + splitPath(path).mkString("/")

  def splitPath(path: String): List[String] =
    path.split("/").filter(_ != "").toList

  def checkPath(path: String): Unit =
    if (path != normalize(path)) throw new VirtualFileSystemException(s"Path '${path}' is invalid")
}

class VirtualFileSystemException(message: String, inner: Option[Throwable]) extends Exception(message, inner.getOrElse(null)) {
  def this() = this("Error", None)
  def this(message: String) = this(message, None)
}

trait VirtualFileSystem {
  def exists(f: VirtualFile): Boolean
  def mode(f: VirtualFile): VirtualFileMode
  def openRead(f: VirtualFile): InputStream
  def openWrite(f: VirtualFile): OutputStream

  def read[T](f: VirtualFile)(inner: InputStream => T): T = using(openRead(f))(s => inner(s))
  def write(f: VirtualFile)(inner: OutputStream => Any): Unit = using(openWrite(f))(s => inner(s))

  private def using[A <: { def close(): Unit }, B](closable: A)(inner: A => B): B = {
    try {
      inner(closable)
    } finally {
      closable.close()
    }
  }
}

class RealVirtualFileSystem(base: File) extends VirtualFileSystem {
  def exists(f: VirtualFile) = <<(f).exists()
  def mode(f: VirtualFile) = if (<<(f).isDirectory) Directory else NonExecutableFile // TODO: handle ExecutableFile
  def openRead(f: VirtualFile) = new FileInputStream(<<(f))
  def openWrite(f: VirtualFile) = new FileOutputStream(<<(f))

  private def /(): String =
    File.separator

  private def <<(f: VirtualFile): File =
    new File(base.getAbsolutePath + / + f.segments.mkString(/))
}

object RealVirtualFileSystem {
  implicit def pathToVirtalFile(path: String)(implicit vfs: RealVirtualFileSystem): VirtualFile =
    new VirtualFile(vfs, path)
}

