package de.choffmeister.secloud.core.utils

import java.io.File
import scala.language.implicitConversions

class RichFile(val file: File) {
  def children: Iterable[File] = new Iterable[File] {
    def iterator = file.isDirectory() match {
      case true => file.listFiles().iterator
      case false => Iterator.empty
    }
  }

  def treeChildrenFirst: Iterable[File] = children.flatMap(new RichFile(_).treeChildrenFirst) ++ Seq(file)
  def treeParentFirst: Iterable[File] = Seq(file) ++ children.flatMap(new RichFile(_).treeParentFirst)
}

object RichFile {
  implicit def fileToRichFile(file: File) = new RichFile(file)
}