package net.secloud.core.objects

import net.secloud.core.crypto._

sealed abstract class ObjectType
case object BlobObjectType extends ObjectType
case object TreeObjectType extends ObjectType
case object CommitObjectType extends ObjectType

case class Issuer(
  name: String,
  publicKey: AsymmetricAlgorithmInstance
)

sealed abstract class BaseObject {
  val id: ObjectId
  val objectType: ObjectType
}

case class Blob(
  id: ObjectId
) extends BaseObject {
  val objectType = BlobObjectType
}

sealed abstract class TreeEntryMode
case object NonExecutableFileTreeEntryMode extends TreeEntryMode
case object ExecutableFileTreeEntryMode extends TreeEntryMode
case object DirectoryTreeEntryMode extends TreeEntryMode

case class TreeEntry(
  id: ObjectId,
  mode: TreeEntryMode,
  name: String,
  key: SymmetricAlgorithmInstance
)

case class Tree(
  id: ObjectId,
  entries: List[TreeEntry]
) extends BaseObject {
  val objectType = TreeObjectType
}

case class Commit(
  id: ObjectId,
  parentIds: List[ObjectId],
  issuers: Map[Seq[Byte], Issuer],
  encapsulatedCommitKeys: Map[Seq[Byte], Seq[Byte]],
  tree: TreeEntry
) extends BaseObject {
  val objectType = CommitObjectType
}
