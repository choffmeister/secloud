package net.secloud.core

import java.io._
import net.secloud.core.objects._
import net.secloud.core.crypto._

class RepositoryFileSystem(db: RepositoryDatabase, commitId: ObjectId, key: Either[SymmetricAlgorithmInstance, AsymmetricAlgorithmInstance]) extends VirtualFileSystem {
  private val commit = db.read(commitId)(dbs => readCommit(dbs, key))

  def exists(f: VirtualFile): Boolean = walkTree(f).isDefined

  def mode(f: VirtualFile): VirtualFileMode = walkTree(f) match {
    case Some(TreeEntry(_, DirectoryTreeEntryMode, _, _)) =>
      Directory
    case Some(TreeEntry(_, FileTreeEntryMode, _, _)) =>
      NonExecutableFile
    case _ =>
      throw new Exception(s"Unknown path $f")
  }

  def children(f: VirtualFile) = walkTree(f) match {
    case Some(TreeEntry(id, DirectoryTreeEntryMode, _, key)) =>
      val tree = db.read(id)(dbs => readTree(dbs, key))
      tree.entries.map(te => f.child(te.name))
    case Some(TreeEntry(_, FileTreeEntryMode, _, _)) =>
      throw new Exception(s"$f is a file and hence cannot have children")
    case _ =>
      throw new Exception(s"Unknown path $f")
  }

  def read[T](f: VirtualFile)(inner: InputStream => T): T = walkTree(f) match {
    case Some(TreeEntry(_, DirectoryTreeEntryMode, _, _)) =>
      throw new Exception(s"$f is a directory and hence cannot be read")
    case Some(TreeEntry(id, FileTreeEntryMode, _, key)) =>
      db.read(id) { dbs =>
        readBlob(dbs)
        readBlobContent(dbs, key) { cs =>
          inner(cs)
        }
      }
    case _ =>
      throw new Exception(s"Unknown path $f")
  }

  def write(f: VirtualFile)(inner: OutputStream => Any): Unit = throw new Exception("Not supported")

  private def walkTree(f: VirtualFile): Option[TreeEntry] = {
    @scala.annotation.tailrec
    def recursion(entry: TreeEntry, f: VirtualFile): Option[TreeEntry] = f.segments match {
      case Nil => Some(entry)
      case next :: rest => entry.mode match {
        case DirectoryTreeEntryMode =>
          val tree = db.read(entry.id)(dbs => readTree(dbs, entry.key))
          tree.entries.find(_.name == next) match {
            case Some(entry) => recursion(entry, VirtualFile.fromSegments(f.segments.tail))
            case _ => None
          }
        case _ => None
      }
    }

    recursion(TreeEntry(commit.tree.id, DirectoryTreeEntryMode, "", commit.tree.key), f)
  }
}
