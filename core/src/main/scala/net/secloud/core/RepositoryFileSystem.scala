package net.secloud.core

import java.io._
import net.secloud.core.objects._
import net.secloud.core.crypto._

class RepositoryFileSystem(db: RepositoryDatabase, commitId: ObjectId, key: Either[SymmetricAlgorithmInstance, AsymmetricAlgorithmInstance]) extends VirtualFileSystem {
  private val commit = db.read(commitId)(dbs => readCommit(dbs, key))
  private val tree = db.read(commit.tree.id)(dbs => readTree(dbs, commit.tree.key))

  def exists(f: VirtualFile) = throw new Exception("Not implemented yet")
  def mode(f: VirtualFile) = throw new Exception("Not implemented yet")
  def children(f: VirtualFile) = walkTree(f).entries.map(te => f.child(te.name))
  def read[T](f: VirtualFile)(inner: InputStream => T): T = {
    val tree = walkTree(f.parent)
    val blobEntry = tree.entries.find(_.name == f.name).get
    db.read(blobEntry.id) { dbs =>
      readBlob(dbs)
      readBlobContent(dbs, blobEntry.key) { cs =>
        inner(cs)
      }
    }
  }
  def write(f: VirtualFile)(inner: OutputStream => Any): Unit = throw new Exception("Not supported")

  private def walkTree(f: VirtualFile): Tree = {
    @scala.annotation.tailrec
    def recursion(t: Tree, f: VirtualFile): Tree = f.segments match {
      case Nil => t
      case next :: rest =>
        val entry = t.entries.find(_.name == next).get
        val innerTree = db.read(entry.id)(dbs => readTree(dbs, entry.key))
        recursion(innerTree, VirtualFile.fromSegments(f.segments.tail))
    }

    recursion(tree, f)
  }
}
