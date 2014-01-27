package net.secloud.core

import java.io.{File, FileInputStream, FileOutputStream}
import net.secloud.core.utils.BinaryReaderWriter._
import net.secloud.core.security.CryptographicAlgorithms._
import net.secloud.core.security.CryptographicAlgorithmSerializer._
import net.secloud.core.objects._

case class RepositoryConfig(val issuer: Issuer)

class Repository(val workingDir: VirtualFileSystem, val database: RepositoryDatabase, val config: RepositoryConfig) {
  def init() {
    database.init()
  }

  def commit(): (Commit, SymmetricEncryptionParameters) = {
    val rootTreeEntry = iterateFiles("/").copy(name = "")
    val headKey = generateKey()
    val commit = Commit(ObjectId.empty, config.issuer, Nil, rootTreeEntry)
    val headId = database.write(s => writeCommit(s, commit, headKey).id)
    (commit.copy(id = headId), headKey)
  }

  private def iterateFiles(path: String): TreeEntry = {
    val file = VirtualFile(path)

    workingDir.mode(file) match {
      case Directory =>
        val entries = workingDir.children(file)
          .filter(e => !e.name.startsWith(".") && e.name != "target")
          .map(e => iterateFiles(e.path))
          .toList
        val key = generateKey()
        val tree = Tree(ObjectId.empty, config.issuer, entries)
        val oid = database.write(s => writeTree(s, tree, key).id)

        TreeEntry(oid, DirectoryTreeEntryMode, file.name, key)
      case NonExecutableFile =>
        val key = generateKey()
        val blob = Blob(ObjectId.empty, config.issuer)
        val oid = database.write(dbs => workingDir.read(file)(fs => writeBlob(dbs, blob, fs, key).id))

        TreeEntry(oid, FileTreeEntryMode, file.name, key)
      case _ => throw new Exception(file.toString)
    }
  }

  private def generateKey() = `AES-128`.generateKey()
}

object Repository {
  def apply(workingDir: VirtualFileSystem, database: RepositoryDatabase, config: RepositoryConfig): Repository =
    new Repository(workingDir, database, config)

  def apply(dir: File, config: RepositoryConfig): Repository =
    new Repository(new NativeFileSystem(dir), new DirectoryRepositoryDatabase(new File(dir, ".secloud")), config)
}
