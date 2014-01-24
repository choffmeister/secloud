package net.secloud.core

import java.io.{File, FileInputStream, FileOutputStream}
import net.secloud.core.utils.BinaryReaderWriter._
import net.secloud.core.security.CryptographicAlgorithms._
import net.secloud.core.security.CryptographicAlgorithmSerializer._
import net.secloud.core.objects._

case class RepositoryConfig(val workingDir: File, val issuer: Issuer)

class Repository(val config: RepositoryConfig, val database: RepositoryDatabase) {
  val algo = `AES-128`

  def init() {
    database.init()
  }

  def commit() {
    val rootTreeEntry = iterateFiles(database, config.issuer, config.workingDir).copy(name = "")
    val headKey = algo.generateKey()
    val commit = Commit(ObjectId.empty, config.issuer, Nil, rootTreeEntry)
    val headId = database.write(s => writeCommit(s, commit, headKey).id)

    saveHead(headId, headKey)
    println(headId)
  }

  def list() {
    val (headId, headKey) = loadHead()
    println(headId)
  }

  def saveHead(headId: ObjectId, headKey: SymmetricEncryptionParameters) {
    val headFile = new File(new File(config.workingDir, ".secloud"), "HEAD")
    val headStream = new FileOutputStream(headFile)
    headStream.writeObjectId(headId)
    writeSymmetricEncryptionParameters(headStream, headKey)
    headStream.close()
  }

  def loadHead(): (ObjectId, SymmetricEncryptionParameters) = {
    val headFile = new File(new File(config.workingDir, ".secloud"), "HEAD")
    val headStream = new FileInputStream(headFile)
    val headId = headStream.readObjectId()
    val headKey = readSymmetricEncryptionParameters(headStream)
    headStream.close()
    (headId, headKey)
  }

  private def iterateFiles(database: RepositoryDatabase, issuer: Issuer, file: File): TreeEntry = {
    if (file.isDirectory) {
      println(file.getAbsolutePath)

      val entries = file.listFiles
        .filter(f => !f.getName.startsWith(".") && f.getName != "target")
        .map(f => iterateFiles(database, issuer, f))
        .toList
      val key = algo.generateKey()
      val tree = Tree(ObjectId.empty, issuer, entries)
      val oid = database.write(s => writeTree(s, tree, key).id)

      TreeEntry(oid, DirectoryTreeEntryMode, file.getName, key)
    } else {
      println(file.getAbsolutePath)

      val fileStream = new FileInputStream(file)
      val key = algo.generateKey()
      val blob = Blob(ObjectId.empty, issuer)
      val oid = database.write(s => writeBlob(s, blob, fileStream, key).id)

      TreeEntry(oid, NonExecutableFileTreeEntryMode, file.getName, key)
    }
  }
}

object Repository {
  def apply(config: RepositoryConfig, database: RepositoryDatabase): Repository =
    new Repository(config, database)

  def apply(config: RepositoryConfig): Repository =
    new Repository(config, new DirectoryRepositoryDatabase(new File(config.workingDir, ".secloud")))
}
