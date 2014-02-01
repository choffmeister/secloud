package net.secloud.core

import java.io.{File, FileInputStream, FileOutputStream}
import net.secloud.core.utils.RichStream._
import net.secloud.core.utils.BinaryReaderWriter._
import net.secloud.core.objects._
import net.secloud.core.crypto._

case class RepositoryConfig(val issuer: Issuer)

class Repository(val workingDir: VirtualFileSystem, val database: RepositoryDatabase, val config: RepositoryConfig) {
  def init() {
    database.init()
  }

  def commit() {
    def commit(f: VirtualFile, head: VirtualFileSystem, wd: VirtualFileSystem): TreeEntry = {
      wd.mode(f) match {
        case Directory =>
          val key = generateKey()
          val entries = wd.children(f)
            .filter(e => !e.name.startsWith(".") && e.name != "target")
            .map(e => commit(f.child(e.name), head, wd))
            .toList
          val tree = Tree(ObjectId(), config.issuer, entries)
          val id = database.write { dbs =>
            signObject(dbs) { ss =>
              writeTree(ss, tree, key)
            }
          }
          TreeEntry(id, DirectoryTreeEntryMode, f.name, key)

        case NonExecutableFile =>
          val key = generateKey()
          val blob = Blob(ObjectId(), config.issuer)
          val id = database.write { dbs =>
            signObject(dbs) { ss =>
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

    commit(VirtualFile("/"), NullFileSystem, workingDir)
  }

  private def generateKey() = AES.generate(32)
}

object Repository {
  def apply(workingDir: VirtualFileSystem, database: RepositoryDatabase, config: RepositoryConfig): Repository =
    new Repository(workingDir, database, config)

  def apply(dir: File, config: RepositoryConfig): Repository =
    new Repository(new NativeFileSystem(dir), new DirectoryRepositoryDatabase(new File(dir, ".secloud")), config)
}
