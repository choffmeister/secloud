package net.secloud.core

import java.io._
import net.secloud.core.crypto._
import net.secloud.core.objects._

class RepositoryFileSystem(db: RepositoryDatabase, commit: Commit) extends VirtualFileSystem {
  def exists(f: VirtualFile): Boolean = walkTree(f).isDefined

  def mode(f: VirtualFile): VirtualFileMode = walkTree(f) match {
    case Some(TreeEntry(_, DirectoryTreeEntryMode, _, _, _)) ⇒
      Directory
    case Some(TreeEntry(_, ExecutableFileTreeEntryMode, _, _, _)) ⇒
      ExecutableFile
    case Some(TreeEntry(_, NonExecutableFileTreeEntryMode, _, _, _)) ⇒
      NonExecutableFile
    case _ ⇒
      throw new Exception(s"Unknown path $f")
  }

  def children(f: VirtualFile) = walkTree(f) match {
    case Some(TreeEntry(id, DirectoryTreeEntryMode, _, key, _)) ⇒
      val tree = db.readTree(id, key)
      tree.entries.map(te ⇒ f.child(te.name))
    case Some(TreeEntry(_, ExecutableFileTreeEntryMode | NonExecutableFileTreeEntryMode, _, _, _)) ⇒
      throw new Exception(s"$f is a file and hence cannot have children")
    case _ ⇒
      throw new Exception(s"Unknown path $f")
  }

  def read[T](f: VirtualFile)(inner: InputStream ⇒ T): T = walkTree(f) match {
    case Some(TreeEntry(_, DirectoryTreeEntryMode, _, _, _)) ⇒
      throw new Exception(s"$f is a directory and hence cannot be read")
    case Some(TreeEntry(id, ExecutableFileTreeEntryMode | NonExecutableFileTreeEntryMode, _, key, _)) ⇒
      db.readBlobContent(id, key)(inner)
    case _ ⇒
      throw new Exception(s"Unknown path $f")
  }

  def write(f: VirtualFile)(inner: OutputStream ⇒ Any): Unit = throw new Exception("Not supported")

  def objOption(f: VirtualFile): Option[BaseObject] = walkTree(f) match {
    case Some(TreeEntry(id, DirectoryTreeEntryMode, _, key, _)) ⇒
      Some(db.readTree(id, key))
    case Some(TreeEntry(id, ExecutableFileTreeEntryMode | NonExecutableFileTreeEntryMode, _, _, _)) ⇒
      Some(db.readBlob(id))
    case _ ⇒
      None
  }

  def treeOption(f: VirtualFile): Option[Tree] = objOption(f) match {
    case Some(t: Tree) ⇒ Some(t)
    case _ ⇒ None
  }

  def blobOption(f: VirtualFile): Option[Blob] = objOption(f) match {
    case Some(b: Blob) ⇒ Some(b)
    case _ ⇒ None
  }

  def obj(f: VirtualFile): BaseObject = objOption(f) match {
    case Some(obj) ⇒ obj
    case _ ⇒ throw new Exception(s"Unknown path $f")
  }

  def tree(f: VirtualFile): Tree = obj(f) match {
    case t: Tree ⇒ t
    case _ ⇒ throw new Exception(s"$f does not point to a tree")
  }

  def blob(f: VirtualFile): Blob = obj(f) match {
    case b: Blob ⇒ b
    case _ ⇒ throw new Exception(s"$f does not point to a blob")
  }

  def key(f: VirtualFile): Option[SymmetricAlgorithmInstance] = walkTree(f) match {
    case Some(TreeEntry(_, _, _, key, _)) ⇒ Some(key)
    case _ ⇒ None
  }

  def hash(f: VirtualFile): Option[Seq[Byte]] = walkTree(f) match {
    case Some(TreeEntry(_, _, _, _, hash)) ⇒ Some(hash)
    case _ ⇒ None
  }

  private def walkTree(f: VirtualFile): Option[TreeEntry] = {
    @scala.annotation.tailrec
    def recursion(entry: TreeEntry, f: VirtualFile): Option[TreeEntry] = f.segments match {
      case Nil ⇒ Some(entry)
      case next :: rest ⇒ entry.mode match {
        case DirectoryTreeEntryMode ⇒
          val tree = db.readTree(entry.id, entry.key)
          tree.entries.find(_.name == next) match {
            case Some(childEntry) ⇒ recursion(childEntry, VirtualFile.fromSegments(f.segments.tail))
            case _ ⇒ None
          }
        case _ ⇒ None
      }
    }

    recursion(TreeEntry(commit.tree.id, DirectoryTreeEntryMode, "", commit.tree.key), f)
  }
}
