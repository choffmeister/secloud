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
  def name = segments.lastOption.getOrElse("")

  def exists: Boolean = vfs.exists(this)
  def mode: VirtualFileMode = vfs.mode(this)
  def children: List[VirtualFile] = vfs.children(this)
  def read[T](inner: InputStream => T): T = vfs.read(this)(inner)
  def write(inner: OutputStream => Any): Unit = vfs.write(this)(inner)
}

object VirtualFile {
  def apply(vfs: VirtualFileSystem, segments: List[String]): VirtualFile =
    VirtualFile(vfs, "/" + segments.mkString("/"))

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
  def children(f: VirtualFile): List[VirtualFile]
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

class NativeFileSystem(base: File) extends VirtualFileSystem {
  def exists(f: VirtualFile) = <<(f).exists()
  def mode(f: VirtualFile) = if (<<(f).isDirectory) Directory else NonExecutableFile // TODO: handle ExecutableFile
  def children(f: VirtualFile) = <<(f).listFiles.map(c => VirtualFile(this, f.segments ++ List(c.getName))).toList
  def openRead(f: VirtualFile) = new FileInputStream(<<(f))
  def openWrite(f: VirtualFile) = new FileOutputStream(<<(f))

  private def /(): String = File.separator
  private def <<(f: VirtualFile): File = new File(base.getAbsolutePath + / + f.segments.mkString(/))
}

object NativeFileSystem {
  implicit def pathToVirtalFile(path: String)(implicit vfs: NativeFileSystem): VirtualFile =
    new VirtualFile(vfs, path)
}

