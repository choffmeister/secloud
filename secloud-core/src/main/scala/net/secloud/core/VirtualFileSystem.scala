package net.secloud.core

import java.io.{ File, FileInputStream, FileOutputStream }
import java.io.{ InputStream, OutputStream }
import net.secloud.core.utils.StreamUtils.using
import scala.language.implicitConversions
import scala.language.reflectiveCalls
import scala.util.matching.Regex

sealed abstract class VirtualFileMode
case object NonExecutableFile extends VirtualFileMode
case object ExecutableFile extends VirtualFileMode
case object Directory extends VirtualFileMode

case class VirtualFile(path: String) {
  VirtualFile.checkPath(path)
  def segments = VirtualFile.splitPath(path)
  def name = segments.lastOption.getOrElse("")

  def child(name: String) = VirtualFile.fromSegments(segments ++ List(name))
  def parent = VirtualFile.fromSegments(segments.take(segments.length - 1).toList)
  def tail = VirtualFile.fromSegments(segments.tail)

  def isChildOf(that: VirtualFile) = this != that && segments.startsWith(that.segments)
  def isParentOf(that: VirtualFile) = this != that && that.segments.startsWith(segments)

  def matches(regex: Regex) = regex.findFirstIn(path).isDefined
}

object VirtualFile {
  def fromSegments(segments: List[String]): VirtualFile =
    VirtualFile("/" + segments.mkString("/"))

  def normalize(path: String): String =
    "/" + splitPath(path).mkString("/")

  def splitPath(path: String): List[String] =
    path.split("/").filter(_ != "").toList

  def checkPath(path: String): Unit =
    if (path != normalize(path)) throw new VirtualFileSystemException(s"Path '${path}' is invalid")

  def fromFile(base: File, file: File): VirtualFile = file.getAbsolutePath.substring(base.getAbsolutePath.length) match {
    case s if s.length > 0 ⇒ VirtualFile(s)
    case s ⇒ VirtualFile("/")
  }
}

class VirtualFileSystemException(message: String, inner: Option[Throwable]) extends Exception(message, inner.getOrElse(null)) {
  def this() = this("Error", None)
  def this(message: String) = this(message, None)
}

trait VirtualFileSystem {
  def exists(f: VirtualFile): Boolean
  def mode(f: VirtualFile): VirtualFileMode
  def children(f: VirtualFile): List[VirtualFile]
  def read[T](f: VirtualFile)(inner: InputStream ⇒ T): T
  def write(f: VirtualFile)(inner: OutputStream ⇒ Any): Unit
}

class NativeFileSystem(base: File) extends VirtualFileSystem {
  def exists(f: VirtualFile) = <<(f).exists()
  def mode(f: VirtualFile) = (<<(f).isDirectory, <<(f).canExecute) match {
    case (true, _) ⇒ Directory
    case (false, true) ⇒ ExecutableFile
    case (false, false) ⇒ NonExecutableFile
  }
  def children(f: VirtualFile) = <<(f).listFiles.map(c ⇒ f.child(c.getName)).toList
  def read[T](f: VirtualFile)(inner: InputStream ⇒ T): T = using(new FileInputStream(<<(f)))(s ⇒ inner(s))
  def write(f: VirtualFile)(inner: OutputStream ⇒ Any): Unit = using(new FileOutputStream(<<(f)))(s ⇒ inner(s))

  private def /(): String = File.separator
  private def <<(f: VirtualFile): File = new File(base.getAbsolutePath + / + f.segments.mkString(/))
}

object NullFileSystem extends VirtualFileSystem {
  def exists(f: VirtualFile) = f.path == "/"
  def mode(f: VirtualFile) = Directory
  def children(f: VirtualFile) = List.empty[VirtualFile]
  def read[T](f: VirtualFile)(inner: InputStream ⇒ T) = throw new Exception("Not supported")
  def write(f: VirtualFile)(inner: OutputStream ⇒ Any) = throw new Exception("Not supported")
}
