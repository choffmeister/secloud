package net.secloud.core

import java.io.{File, FileInputStream, FileOutputStream}
import net.secloud.core.utils.RichStream._
import net.secloud.core.utils.StreamUtils._
import net.secloud.core.utils.BinaryReaderWriter._
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

  def commit(): Commit = {
    val key = generateKey()
    val keyEncoded = streamAsBytes(s => key.algorithm.save(s, key))

    val parents = List.empty[ObjectId]
    val issuers = List(config.asymmetricKey).map(rsa => (RSA.fingerprint(rsa).toSeq, Issuer("Issuer", rsa))).toMap
    val encapsulatedCommitKeys = issuers.map(i => (i._1, i._2.publicKey.wrapKey(keyEncoded).toSeq))
    val tree = snapshot()

    val commitRaw = Commit(ObjectId.empty, parents, issuers, encapsulatedCommitKeys, tree)
    val commitId = database.write { dbs =>
      signObject(dbs, config.asymmetricKey) { ss =>
        writeCommit(ss, commitRaw, key)
      }
    }

    commitRaw.copy(id = commitId)
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
                  fs.pipeTo(bs)
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
