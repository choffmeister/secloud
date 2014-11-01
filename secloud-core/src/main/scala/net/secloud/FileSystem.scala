package net.secloud

import java.io._

case class Path(segments: List[String]) {
  def path = "/" + segments.mkString("/")
  def name = segments.lastOption.getOrElse("")

  def child(name: String) = Path(segments ++ List(name))
  def parent = Path(segments.take(segments.length - 1).toList)

  def isChildOf(that: Path) = this != that && segments.startsWith(that.segments)
  def isParentOf(that: Path) = this != that && that.segments.startsWith(segments)

  override def toString = path
}

object Path {
  def apply(segments: String*): Path = Path(segments.toList)
  def apply(path: String): Path = Path(path.split("/", -1).filter(_ != "").toList)
}

trait FileSystem {
  def exists(p: Path): Boolean
  def timestamp(p: Path): Long
  def mkdir(p: Path, recursive: Boolean = false): Boolean
  def children(p: Path): List[Path]
  def read[T](p: Path)(inner: InputStream ⇒ T): T
  def write(p: Path)(inner: OutputStream ⇒ Any): Unit
}

class RichFileSystem(inner: FileSystem) {
  def readBytes(p: Path): Array[Byte] = {
    val temp = new ByteArrayOutputStream()
    inner.read(p)(s ⇒ copyStream(s, temp))
    temp.toByteArray
  }

  def writeBytes(p: Path, b: Array[Byte]): Unit = {
    val temp = new ByteArrayInputStream(b)
    inner.write(p)(s ⇒ copyStream(temp, s))
  }

  def readString(p: Path): String = new String(readBytes(p), "UTF-8")

  def writeString(p: Path, s: String) = writeBytes(p, s.getBytes("UTF-8"))

  def descendants(p: Path): Iterable[Path] = new Iterable[Path] {
    def iterator = new Iterator[Path] {
      private val stack = scala.collection.mutable.Stack(p)
      next()

      def hasNext: Boolean = stack.nonEmpty
      def next(): Path = {
        val current = stack.pop()
        inner.children(current).foreach(stack.push(_))
        current
      }
    }
  }

  private def copyStream(input: InputStream, output: OutputStream, bufferSize: Int = 8192): Unit = {
    val buffer = new Array[Byte](bufferSize)
    var done = false
    while (!done) {
      val read = input.read(buffer, 0, bufferSize)
      if (read > 0) output.write(buffer, 0, read)
      else done = true
    }
  }
}

object RichFileSystem {
  implicit def toRichFileSystem(fs: FileSystem): RichFileSystem = new RichFileSystem(fs)
}

class NativeFileSystem(base: File) extends FileSystem {
  private implicit def pathToFile(p: Path) = new File(base, p.path.drop(1))

  override def exists(p: Path) = p.exists()

  override def timestamp(p: Path) = p.lastModified()

  override def mkdir(p: Path, recursive: Boolean = false) = if (recursive) p.mkdirs() else p.mkdir()

  override def children(p: Path) = Option(p.listFiles).map(_.toList).getOrElse(List.empty).map(c ⇒ p.child(c.getName))

  override def read[T](p: Path)(inner: InputStream ⇒ T) = {
    val is = new FileInputStream(p)
    try inner(is)
    finally is.close()
  }

  override def write(p: Path)(inner: OutputStream ⇒ Any) = {
    val os = new FileOutputStream(p)
    try inner(os)
    finally os.close()
  }
}
