package de.choffmeister.secloud.core.security

object CryptographicAlgorithms {
  sealed abstract class SymmetricEncryptionAlgorithm {
    val algorithmName: String
    val blockModeName: String
    val paddingName: String
    val fullAlgorithmName: String = s"$algorithmName/$blockModeName/$paddingName"

    val blockSize: Int
    val keySize: Int
  }

  sealed abstract class AES {
    val algorithmName = "AES"
    val blockModeName = "CBC"
    val paddingName = "NoPadding"

    val blockSize = 16
  }

  case object `AES-128` extends AES {
    val keySize = 16
  }

  case object `AES-192` extends AES {
    val keySize = 24
  }

  case object `AES-256` extends AES {
    val keySize = 32
  }
  
  sealed abstract class HashAlgorithm {
    val algorithmName: String
  }
  
  case object `SHA-1` extends HashAlgorithm {
    val algorithmName = "SHA-1"
  }

  sealed abstract class `SHA-2` extends HashAlgorithm

  case object `SHA-2-256` extends `SHA-2` {
    val algorithmName = "SHA-256"
  }

  case object `SHA-2-384` extends `SHA-2` {
    val algorithmName = "SHA-384"
  }

  case object `SHA-2-512` extends `SHA-2` {
    val algorithmName = "SHA-512"
  }
}