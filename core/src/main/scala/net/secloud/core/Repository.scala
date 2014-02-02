package net.secloud.core

import java.io._
import net.secloud.core.utils._
import net.secloud.core.utils.StreamUtils._
import net.secloud.core.objects._
import net.secloud.core.crypto._

case class RepositoryConfig(
  val asymmetricKey: AsymmetricAlgorithmInstance,
  val symmetricAlgorithm: SymmetricAlgorithm,
  val symmetricAlgorithmKeySize: Int
)

class Repository(val workingDir: VirtualFileSystem, val database: RepositoryDatabase, val config: RepositoryConfig) {
  def init() {
    database.init()
  }

  def commit(): ObjectId = {
    val key = generateKey()
    val keyEncoded = streamAsBytes(s => key.algorithm.save(s, key))

    val parents = List.empty[ObjectId]
    val issuers = List(config.asymmetricKey).map(rsa => (RSA.fingerprint(rsa).toSeq, Issuer("Issuer", rsa))).toMap
    val tree = snapshot()

    val commitRaw = Commit(ObjectId.empty, parents, issuers, Map.empty, tree)
    val commitId = database.write { dbs =>
      signObject(dbs, config.asymmetricKey) { ss =>
        writeCommit(ss, commitRaw, key)
      }
    }

    database.head = commitId
    commitId
  }

  def traverse(f: VirtualFile, current: Option[BaseObject] = None): BaseObject = {
    current match {
      case Some(current) => current match {
        case c: Commit => database.read(c.tree.id)(dbs => traverse(f, Some(readTree(dbs, c.tree.key))))
        case t: Tree => f.segments match {
          case first :: tail =>
            t.entries.find(_.name == first) match {
              case Some(e) => e.mode match {
                case DirectoryTreeEntryMode => database.read(e.id)(dbs => traverse(f.tail, Some(readTree(dbs, e.key))))
                case FileTreeEntryMode => database.read(e.id)(dbs => traverse(f.tail, Some(readBlob(dbs))))
              }
              case None => throw new Exception("Invalid path")
            }

          case Nil => t
        }
        case b: Blob => f.segments match {
          case first :: tail => throw new Exception("Invalid path")
          case Nil => b
        }
        case _ => throw new Exception("Unsupported object")
      }
      case None =>
        val head = database.head
        val commit = database.read(head)(dbs => readCommit(dbs, Right(config.asymmetricKey))).copy(id = head)
        traverse(f, Some(commit))
    }
  }

  def read[T](f: VirtualFile, commit: Option[Commit] = None)(inner: InputStream => T): T = {
    val parentTree = traverse(f.parent, commit).asInstanceOf[Tree]
    val blob = traverse(VirtualFile.fromSegments(List(f.name)), Some(parentTree)).asInstanceOf[Blob]
    val blobEntry = parentTree.entries.find(_.name == f.name).get

    database.read(blobEntry.id) { dbs =>
      readBlob(dbs)
      readBlobContent(dbs, blobEntry.key)(inner)
    }
  }

  def snapshot(): TreeEntry = {
    def recursion(f: VirtualFile, head: VirtualFileSystem, wd: VirtualFileSystem): TreeEntry = {
      wd.mode(f) match {
        case Directory =>
          val key = generateKey()
          val entries = wd.children(f)
            .filter(e => !e.name.startsWith(".") && e.name != "target")
            .map(e => recursion(f.child(e.name), head, wd))
            .toList
          val tree = Tree(ObjectId(), entries)
          val id = database.write { dbs =>
            signObject(dbs, config.asymmetricKey) { ss =>
              writeTree(ss, tree, key)
            }
          }
          TreeEntry(id, DirectoryTreeEntryMode, f.name, key)

        case NonExecutableFile =>
          val key = generateKey()
          val blob = Blob(ObjectId())
          val id = database.write { dbs =>
            signObject(dbs, config.asymmetricKey) { ss =>
              writeBlob(ss, blob)
              writeBlobContent(ss, key) { bs =>
                wd.read(f) { fs =>
                  pipeStream(fs, bs)
                }
              }
            }
          }
          TreeEntry(id, FileTreeEntryMode, f.name, key)

        case _ => throw new Exception()
      }
    }

    recursion(VirtualFile("/"), NullFileSystem, workingDir)
  }

  private def generateKey() = config.symmetricAlgorithm.generate(config.symmetricAlgorithmKeySize)
}

object Repository {
  def apply(workingDir: VirtualFileSystem, database: RepositoryDatabase, config: RepositoryConfig): Repository =
    new Repository(workingDir, database, config)

  def apply(dir: File, config: RepositoryConfig): Repository =
    new Repository(new NativeFileSystem(dir), new DirectoryRepositoryDatabase(new File(dir, ".secloud")), config)
}
