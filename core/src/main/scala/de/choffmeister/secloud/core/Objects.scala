package de.choffmeister.secloud.core

case class Issuer(id: Seq[Byte], name: String)

object Issuer {
  def apply(): Issuer = Issuer(Seq.empty[Byte], "")
}

abstract class BaseObject {
  val id: ObjectId
  val issuer: Issuer
  val objectType: ObjectSerializerConstants.ObjectType
}

case class Blob(
  id: ObjectId,
  issuer: Issuer
) extends BaseObject {
  val objectType = ObjectSerializerConstants.BlobObjectType
}

case class Tree(
  id: ObjectId,
  issuer: Issuer
) extends BaseObject {
  val objectType = ObjectSerializerConstants.TreeObjectType
}

case class Commit(
  id: ObjectId,
  issuer: Issuer
) extends BaseObject {
  val objectType = ObjectSerializerConstants.CommitObjectType
}