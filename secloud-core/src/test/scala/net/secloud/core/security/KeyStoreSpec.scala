package net.secloud.core.security

import org.specs2.mutable._

class KeyStoreSpec extends Specification {
  "KeyStrore" should {
    "save and load private keys" in {
      val randomString = randomKeyName

      KeyStore.savePrivateKey(randomString, "vErYsEcReTpAsSwOrD")
      KeyStore.loadPrivateKey(randomString) === Some("vErYsEcReTpAsSwOrD")
      KeyStore.removePrivateKey(randomString)
      ok
    }

    "return none if private key does not exist" in {
      val randomString = randomKeyName

      KeyStore.loadPrivateKey(randomString) must beNone
    }

    "remove private keys" in {
      val randomString = randomKeyName

      KeyStore.savePrivateKey(randomString, "vErYsEcReTpAsSwOrD")
      KeyStore.loadPrivateKey(randomString) must beSome
      KeyStore.removePrivateKey(randomString)
      KeyStore.loadPrivateKey(randomString) must beNone
    }

    "override existing private keys" in {
      val randomString = randomKeyName

      KeyStore.savePrivateKey(randomString, "vErYsEcReTpAsSwOrD")
      KeyStore.loadPrivateKey(randomString) === Some("vErYsEcReTpAsSwOrD")
      KeyStore.savePrivateKey(randomString, "vErYsEcReTpAsSwOrD-2")
      KeyStore.loadPrivateKey(randomString) === Some("vErYsEcReTpAsSwOrD-2")
      KeyStore.removePrivateKey(randomString)
      ok
    }
  }

  def randomKeyName = java.util.UUID.randomUUID().toString()
}
