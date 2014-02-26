package net.secloud.core.objects

private[objects] object ObjectSerializerConstants {
  val MagicBytes = 0x12345678

  sealed abstract class BlockType
  case object PublicBlockType extends BlockType
  case object PrivateBlockType extends BlockType
  case object SignatureBlockType extends BlockType

  val blockTypeMap = Map[BlockType, Byte](
    PublicBlockType -> 0x00,
    PrivateBlockType -> 0x01,
    SignatureBlockType -> 0x02
  )
  val blockTypeMapInverse = blockTypeMap.map(entry => (entry._2, entry._1))

  val objectTypeMap = Map[ObjectType, Byte](
    BlobObjectType -> 0x00,
    TreeObjectType -> 0x01,
    CommitObjectType -> 0x02
  )
  val objectTypeMapInverse = objectTypeMap.map(entry => (entry._2, entry._1))

  val treeEntryModeMap = Map[TreeEntryMode, Byte](
    DirectoryTreeEntryMode -> 0x00,
    ExecutableFileTreeEntryMode -> 0x01,
    NonExecutableFileTreeEntryMode -> 0x02
  )
  val treeEntryModeMapInverse = treeEntryModeMap.map(entry => (entry._2, entry._1))
}
