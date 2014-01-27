package net.secloud.core.security

case object `SHA-1` extends HashAlgorithm {
  val friendlyName = "SHA-1"
  val algorithmName = "SHA-1"
}

sealed abstract class `SHA-2` extends HashAlgorithm

case object `SHA-2-256` extends `SHA-2` {
  val friendlyName = "SHA-2-256"
  val algorithmName = "SHA-256"
}

case object `SHA-2-384` extends `SHA-2` {
  val friendlyName = "SHA-2-384"
  val algorithmName = "SHA-384"
}

case object `SHA-2-512` extends `SHA-2` {
  val friendlyName = "SHA-2-512"
  val algorithmName = "SHA-512"
}
