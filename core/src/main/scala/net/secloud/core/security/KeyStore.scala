package net.secloud.core.security

import org.netbeans.api.keyring.Keyring

object KeyStore {
  def keyStoreEntryName(name: String): String = s"secloud-$name"

  def savePrivateKey(name: String, key: String) {
    Keyring.save(keyStoreEntryName(name), key.toCharArray, s"secloud key for $name")
  }

  def loadPrivateKey(name: String): Option[String] = Option(Keyring.read(keyStoreEntryName(name))) match {
    case Some(charArray) ⇒ Some(charArray.mkString)
    case _ ⇒ None
  }

  def removePrivateKey(name: String) = Keyring.delete(keyStoreEntryName(name))
}
