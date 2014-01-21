package net.secloud.core.utils

import org.specs2.mutable._
import org.specs2.matcher._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import java.io.File
import java.util.UUID

@RunWith(classOf[JUnitRunner])
class RichFileSpec extends Specification {
  def getTempDir = new File(new File(System.getProperty("java.io.tmpdir")), UUID.randomUUID().toString())

  val base = getTempDir
    val dir1 = new File(base, "a-sub1")
      val dir2 = new File(dir1, "z-subsub1")
        val file1 = new File(dir2, "file1")
        val file2 = new File(dir2, "file2")
      val file3 = new File(dir1, "file3")
    val file4 = new File(base, "file4")

  base.mkdirs()
  dir1.mkdir()
  dir2.mkdir()
  file1.createNewFile()
  file2.createNewFile()
  file3.createNewFile()
  file4.createNewFile()

  "iterate with children before their parents" in {
    val list = new RichFile(base).treeChildrenFirst.toList

    list must haveSize(7)
    list must occureBefore(file1, dir2)
    list must occureBefore(file2, dir2)
    list must occureBefore(dir2, dir1)
    list must occureBefore(file3, dir1)
    list must occureBefore(dir1, base)
    list must occureBefore(file4, base)

    val listFiltered = new RichFile(base).treeChildrenFirstFiltered(_.getName != "z-subsub1").toList
    listFiltered must haveSize(4)
    listFiltered must occureBefore(file3, dir1)
    listFiltered must occureBefore(dir1, base)
    listFiltered must occureBefore(file4, base)
  }

  "iterate with parents before their children" in {
    val list = new RichFile(base).treeParentFirst.toList

    list must haveSize(7)
    list must occureBefore(dir2, file1)
    list must occureBefore(dir2, file2)
    list must occureBefore(dir1, dir2)
    list must occureBefore(dir1, file3)
    list must occureBefore(base, dir1)
    list must occureBefore(base, file4)

    val listFiltered = new RichFile(base).treeParentFirstFiltered(_.getName != "z-subsub1").toList
    listFiltered must haveSize(4)
    listFiltered must occureBefore(dir1, file3)
    listFiltered must occureBefore(base, dir1)
    listFiltered must occureBefore(base, file4)
  }

  def occureBefore[T](a: T, b: T): Matcher[Seq[T]] = (s: Seq[T]) => s.takeWhile(_ != b) must contain(a)
}
