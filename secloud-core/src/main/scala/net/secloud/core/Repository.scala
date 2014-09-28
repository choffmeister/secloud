package net.secloud.core

import java.io._
import net.secloud.core.backend.Backend
import net.secloud.core.crypto._
import net.secloud.core.objects._
import net.secloud.core.objects.ObjectSerializer._
import net.secloud.core.utils.StreamUtils._

class Repository(val workingDir: VirtualFileSystem, val database: RepositoryDatabase, val config: Config) {
  private lazy val log = org.slf4j.LoggerFactory.getLogger(getClass)

  def init(): ObjectId = {
    log.info("Initializing a new repository")
    database.init()

    val key = generateKey()
    val tree = Tree(ObjectId(), Nil)
    val treeId = database.writeTree(tree, config.asymmetricKey, key)
    val commitId = commit(Nil, TreeEntry(treeId, DirectoryTreeEntryMode, "", key))

    database.headId = commitId
    commitId
  }

  def commit(changeHints: List[VirtualFile] = Nil): ObjectId = {
    val treeEntry = snapshot(changeHints)
    commit(List(headId), treeEntry)
  }

  def commit(parentIds: List[ObjectId], treeEntry: TreeEntry): ObjectId = {
    if (parentIds.length == 1) {
      val parent = database.readCommit(parentIds(0), Right(config.asymmetricKey))
      if (parent.tree.id == treeEntry.id) return parent.id
    }

    log.info("Committing")
    val key = generateKey()
    val issuers = List(config.asymmetricKey).map(apk ⇒ (apk.fingerprint.toSeq, Issuer("Issuer", apk))).toMap
    val commit = Commit(ObjectId.empty, parentIds, issuers, Map.empty, treeEntry)
    val commitId = database.writeCommit(commit, config.asymmetricKey, key)

    database.headId = commitId
    commitId
  }

  def snapshot(changeHints: List[VirtualFile]): TreeEntry = {
    def recursion(f: VirtualFile, head: RepositoryFileSystem, wd: VirtualFileSystem): TreeEntry = {
      if (changeHints.isEmpty || changeHints.exists(ch ⇒ ch == f || ch.isChildOf(f) || ch.isParentOf(f))) {
        wd.mode(f) match {
          case Directory ⇒
            val entries = wd.children(f)
              .filter(e ⇒ !e.name.startsWith(".") && config.ignore.forall(i ⇒ (f.child(e.name)).matches(i) == false))
              .map(e ⇒ recursion(f.child(e.name), head, wd))
              .sortBy(e ⇒ e.name)
              .toList

            head.treeOption(f) match {
              case Some(t: Tree) if t.entries.map(e ⇒ (e.id, e.name, e.mode)) == entries.map(e ⇒ (e.id, e.name, e.mode)) ⇒
                TreeEntry(t.id, DirectoryTreeEntryMode, f.name, head.key(f).get)
              case _ ⇒
                val tree = Tree(ObjectId(), entries)
                val key = generateKey()
                val id = database.writeTree(tree, config.asymmetricKey, key)
                TreeEntry(id, DirectoryTreeEntryMode, f.name, key)
            }

          case mode @ (NonExecutableFile | ExecutableFile) ⇒
            val blob = Blob(ObjectId())
            val key = generateKey()

            database.writeExplicit { (dbs, writer) ⇒
              var hash = Seq.empty[Byte]
              val id = signObject(dbs, config.asymmetricKey) { ss ⇒
                writeBlob(ss, blob)
                writeBlobContent(ss, key) { cs ⇒
                  hash = wd.read(f)(fs ⇒ SHA1.create().hash(cs)(hs ⇒ pipeStream(fs, hs)).toSeq)
                }
              }
              val treeEntryMode = mode match {
                case NonExecutableFile ⇒ NonExecutableFileTreeEntryMode
                case ExecutableFile ⇒ ExecutableFileTreeEntryMode
                case _ ⇒ throw new Exception()
              }

              head.hash(f) match {
                case Some(prevHash) if prevHash == hash ⇒
                  writer.dismiss()
                  val prevBlob = head.blob(f)
                  TreeEntry(prevBlob.id, treeEntryMode, f.name, head.key(f).get, prevHash)
                case _ ⇒
                  writer.persist(id)
                  TreeEntry(id, treeEntryMode, f.name, key, hash)
              }
            }

          case _ ⇒ throw new Exception()
        }
      } else {
        log.debug("Reusing entry for {}", f.path)
        head.tree(f.parent).entries.find(_.name == f.name).get
      }
    }

    recursion(VirtualFile("/"), fileSystem(headCommit), workingDir)
  }

  def synchronize(backend: Backend): Unit = {
    def recursion(entry: TreeEntry, depth: Int): Unit = {
      if (!backend.has(entry.id)) {
        entry.mode match {
          case DirectoryTreeEntryMode ⇒
            println("  " + "  " * depth + "tree " + entry.id.hex)
            database.readTreePublic(entry.id).entries.foreach(e ⇒ recursion(e, depth + 1))
            database.read(entry.id)(s ⇒ backend.put(entry.id, s))
          case NonExecutableFileTreeEntryMode | ExecutableFileTreeEntryMode ⇒
            println("  " + "  " * depth + "blob " + entry.id.hex)
            database.read(entry.id)(s ⇒ backend.put(entry.id, s))
        }
      }
    }

    val queue = scala.collection.mutable.Queue.empty[Commit]
    queue.enqueue(database.readCommitPublic(headId))

    while (queue.nonEmpty) {
      val commit = queue.dequeue()
      commit.parentIds.foreach(pid ⇒ queue.enqueue(database.readCommitPublic(pid)))

      println("commit " + commit.id.hex)
      if (!backend.has(commit.id)) {
        recursion(commit.tree, 0)
        database.read(commit.id)(s ⇒ backend.put(commit.id, s))
      }
    }

    backend.headId = headId
  }

  def headId: ObjectId = database.headId
  def headCommit: Commit = database.readCommit(headId, Right(config.asymmetricKey))
  def fileSystem(commit: Commit): RepositoryFileSystem = new RepositoryFileSystem(database, commit)

  private def generateKey() = config.symmetricAlgorithm.generate(config.symmetricAlgorithmKeySize)
}

object Repository {
  def apply(workingDir: VirtualFileSystem, database: RepositoryDatabase, config: Config): Repository =
    new Repository(workingDir, database, config)

  def apply(dir: File, config: Config): Repository =
    new Repository(new NativeFileSystem(dir), new DirectoryRepositoryDatabase(new File(dir, ".secloud")), config)
}
