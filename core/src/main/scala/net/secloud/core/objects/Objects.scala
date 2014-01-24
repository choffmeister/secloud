package net.secloud.core.objects

import net.secloud.core.security.CryptographicAlgorithms._

sealed abstract class ObjectType
case object BlobObjectType extends ObjectType
case object TreeObjectType extends ObjectType
case object CommitObjectType extends ObjectType

case class Issuer(id: Seq[Byte], name: String)

sealed abstract class BaseObject {
  val id: ObjectId
  val issuer: Issuer
  val objectType: ObjectType
}

case class Blob(
  id: ObjectId,
  issuer: Issuer
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
  key: SymmetricEncryptionParameters
)

case class Tree(
  id: ObjectId,
  issuer: Issuer,
  entries: List[TreeEntry]
) extends BaseObject {
  val objectType = TreeObjectType
}

case class CommitParent(
  id: ObjectId,
  key: SymmetricEncryptionParameters
)

case class Commit(
  id: ObjectId,
  issuer: Issuer,
  parents: List[CommitParent],
  tree: TreeEntry
) extends BaseObject {
  val objectType = CommitObjectType
}