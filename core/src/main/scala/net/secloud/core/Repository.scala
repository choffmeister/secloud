package net.secloud.core

import java.io.{File, FileInputStream, FileOutputStream}
import net.secloud.core.utils.BinaryReaderWriter._
import net.secloud.core.security.CryptographicAlgorithms._
import net.secloud.core.security.CryptographicAlgorithmSerializer._
import net.secloud.core.objects._

case class RepositoryConfig(val issuer: Issuer)

class Repository(val workingDir: RepositoryWorkingDir, val database: RepositoryDatabase, val config: RepositoryConfig) {
  def init() {
    workingDir.init()
    database.init()
  }

  def commit() {
    val rootTreeEntry = iterateFiles("/").copy(name = "")
    val headKey = generateKey()
    val commit = Commit(ObjectId.empty, config.issuer, Nil, rootTreeEntry)
    val headId = database.write(s => writeCommit(s, commit, headKey).id)

    println(headId)
  }

  private def iterateFiles(path: String): TreeEntry = {
    val element = workingDir.pathToElement(path)

    element.mode match {
      case DirectoryElementMode =>
        val entries = workingDir.list(element)
          .filter(e => !e.name.startsWith(".") && e.name != "target")
          .map(e => iterateFiles(e.path))
          .toList
        val key = generateKey()
        val tree = Tree(ObjectId.empty, config.issuer, entries)
        val oid = database.write(s => writeTree(s, tree, key).id)

        TreeEntry(oid, DirectoryTreeEntryMode, element.name, key)
      case NonExecutableFileElementMode =>
        val key = generateKey()
        val blob = Blob(ObjectId.empty, config.issuer)
        val oid = database.write { s1 =>
          workingDir.read(element) { s2 =>
            writeBlob(s1, blob, s2, key).id
          }
        }

        TreeEntry(oid, NonExecutableFileTreeEntryMode, element.name, key)
      case _ => throw new Exception(element.toString)
    }
  }

  private def generateKey() = `AES-128`.generateKey()
}

object Repository {
  def apply(workingDir: RepositoryWorkingDir, database: RepositoryDatabase, config: RepositoryConfig): Repository =
    new Repository(workingDir, database, config)

  def apply(dir: File, config: RepositoryConfig): Repository =
    new Repository(new DirectoryRepositoryWorkingDir(dir), new DirectoryRepositoryDatabase(new File(dir, ".secloud")), config)
}
