package net.secloud.core

import java.io._
import net.secloud.core.crypto._
import net.secloud.core.objects._
import net.secloud.core.utils.StreamUtils._

case class RepositoryConfig(
  val asymmetricKey: AsymmetricAlgorithmInstance,
  val symmetricAlgorithm: SymmetricAlgorithm,
  val symmetricAlgorithmKeySize: Int)

class Repository(val workingDir: VirtualFileSystem, val database: RepositoryDatabase, val config: RepositoryConfig) {
  private lazy val log = org.slf4j.LoggerFactory.getLogger(getClass)

  def init(): ObjectId = {
    log.info("Initializing a new repository")
    database.init()

    val key = generateKey()
    val tree = Tree(ObjectId(), Nil)
    val treeId = database.write { dbs ⇒
      signObject(dbs, config.asymmetricKey) { ss ⇒
        writeTree(ss, tree, key)
      }
    }
    val commitId = commit(treeId, key)

    database.head = commitId
    commitId
  }

  def commit(treeId: ObjectId, treeKey: SymmetricAlgorithmInstance): ObjectId = {
    log.info("Committing")
    val key = generateKey()
    val parents = List.empty[ObjectId]
    val issuers = List(config.asymmetricKey).map(apk ⇒ (apk.fingerprint.toSeq, Issuer("Issuer", apk))).toMap
    val treeEntry = TreeEntry(treeId, DirectoryTreeEntryMode, "", treeKey)

    val commitRaw = Commit(ObjectId.empty, parents, issuers, Map.empty, treeEntry)
    val commitId = database.write { dbs ⇒
      signObject(dbs, config.asymmetricKey) { ss ⇒
        writeCommit(ss, commitRaw, key)
      }
    }

    database.head = commitId
    commitId
  }

  def snapshot(): TreeEntry = {
    def recursion(f: VirtualFile, head: VirtualFileSystem, wd: VirtualFileSystem): TreeEntry = {
      wd.mode(f) match {
        case Directory ⇒
          val key = generateKey()
          val entries = wd.children(f)
            .filter(e ⇒ !e.name.startsWith(".") && e.name != "target")
            .map(e ⇒ recursion(f.child(e.name), head, wd))
            .toList
          val tree = Tree(ObjectId(), entries)
          val id = database.write { dbs ⇒
            signObject(dbs, config.asymmetricKey) { ss ⇒
              writeTree(ss, tree, key)
            }
          }
          TreeEntry(id, DirectoryTreeEntryMode, f.name, key)

        case mode @ (NonExecutableFile | ExecutableFile) ⇒
          val key = generateKey()
          val blob = Blob(ObjectId())
          val id = database.write { dbs ⇒
            signObject(dbs, config.asymmetricKey) { ss ⇒
              writeBlob(ss, blob)
              writeBlobContent(ss, key) { bs ⇒
                wd.read(f) { fs ⇒
                  pipeStream(fs, bs)
                }
              }
            }
          }
          val treeEntryMode = mode match {
            case NonExecutableFile ⇒ NonExecutableFileTreeEntryMode
            case ExecutableFile ⇒ ExecutableFileTreeEntryMode
            case _ ⇒ throw new Exception()
          }
          TreeEntry(id, treeEntryMode, f.name, key)

        case _ ⇒ throw new Exception()
      }
    }

    recursion(VirtualFile("/"), NullFileSystem, workingDir)
  }

  def head: ObjectId = database.head
  def fileSystem(commitId: ObjectId): RepositoryFileSystem = new RepositoryFileSystem(database, commitId, Right(config.asymmetricKey))

  private def generateKey() = config.symmetricAlgorithm.generate(config.symmetricAlgorithmKeySize)
}

object Repository {
  def apply(workingDir: VirtualFileSystem, database: RepositoryDatabase, config: RepositoryConfig): Repository =
    new Repository(workingDir, database, config)

  def apply(dir: File, config: RepositoryConfig): Repository =
    new Repository(new NativeFileSystem(dir), new DirectoryRepositoryDatabase(new File(dir, ".secloud")), config)
}
