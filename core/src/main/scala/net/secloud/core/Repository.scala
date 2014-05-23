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
  def init(): ObjectId = {
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
    def recursion(f: VirtualFile, head: RepositoryFileSystem, wd: VirtualFileSystem): TreeEntry = {
      wd.mode(f) match {
        case Directory ⇒
          val entries = wd.children(f)
            .filter(e ⇒ !e.name.startsWith(".") && e.name != "target")
            .map(e ⇒ recursion(f.child(e.name), head, wd))
            .sortBy(e ⇒ e.name)
            .toList
          val (key, id) = head.treeOption(f) match {
            case Some(t: Tree) if t.entries.map(e ⇒ (e.id, e.name, e.mode)) == entries.map(e ⇒ (e.id, e.name, e.mode)) ⇒
              val key = head.key(f).get
              val id = t.id
              (key, id)
            case _ ⇒
              val tree = Tree(ObjectId(), entries)
              val key = generateKey()
              val id = database.write { dbs ⇒
                signObject(dbs, config.asymmetricKey) { ss ⇒
                  writeTree(ss, tree, key)
                }
              }
              (key, id)
          }
          TreeEntry(id, DirectoryTreeEntryMode, f.name, key)

        case mode @ (NonExecutableFile | ExecutableFile) ⇒
          val (key, id) = head.blobOption(f) match {
            // TODO: properly detect if found blob can be reused
            case Some(b: Blob) if true ⇒
              val key = head.key(f).get
              val id = b.id
              (key, id)
            case _ ⇒
              val blob = Blob(ObjectId())
              val key = generateKey()
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
              (key, id)
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

    recursion(VirtualFile("/"), fileSystem(head), workingDir)
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
