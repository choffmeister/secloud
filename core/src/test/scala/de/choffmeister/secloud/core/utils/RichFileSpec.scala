package de.choffmeister.secloud.core.utils

import org.specs2.mutable._
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
    list.map(_.getName) === Seq(file1, file2, dir2, file3, dir1, file4, base).map(_.getName)
    
    val listFiltered = new RichFile(base).treeChildrenFirstFiltered(_.getName != "z-subsub1")
    listFiltered.map(_.getName) === Seq(file3, dir1, file4, base).map(_.getName)
  }

  "iterate with parents before their children" in {
    val list = new RichFile(base).treeParentFirst.toList
    list.map(_.getName) === Seq(base, dir1, dir2, file1, file2, file3, file4).map(_.getName)
    
    val listFiltered = new RichFile(base).treeParentFirstFiltered(_.getName != "z-subsub1")
    listFiltered.map(_.getName) === Seq(base, dir1, file3, file4).map(_.getName)
  }
}