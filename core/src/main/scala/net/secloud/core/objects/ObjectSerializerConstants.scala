package net.secloud.core.objects

private[objects] object ObjectSerializerConstants {
  val MagicBytes = 0x12345678

  sealed abstract class BlockType
  case object IssuerIdentityBlockType extends BlockType
  case object IssuerSignatureBlockType extends BlockType
  case object PublicBlockType extends BlockType
  case object PrivateBlockType extends BlockType

  val blockTypeMap = Map[BlockType, Byte](
    IssuerIdentityBlockType -> 0x00,
    IssuerSignatureBlockType -> 0x01,
    PublicBlockType -> 0x02,
    PrivateBlockType -> 0x03
  )
  val blockTypeMapInverse = blockTypeMap.map(entry => (entry._2, entry._1))

  val objectTypeMap = Map[ObjectType, Byte](
    BlobObjectType -> 0x00,
    TreeObjectType -> 0x01,
    CommitObjectType -> 0x02
  )
  val objectTypeMapInverse = objectTypeMap.map(entry => (entry._2, entry._1))

  val treeEntryModeMap = Map[TreeEntryMode, Byte](
    NonExecutableFileTreeEntryMode -> 0x00,
    ExecutableFileTreeEntryMode -> 0x01,
    DirectoryTreeEntryMode -> 0x10
  )
  val treeEntryModeMapInverse = treeEntryModeMap.map(entry => (entry._2, entry._1))
}
