package net.secloud.core.crypto

import java.security.SecureRandom

object RandomGenerator {
  lazy val random: SecureRandom = new SecureRandom()

  def nextBytes(length: Int): Array[Byte] = {
    val buffer = new Array[Byte](length)
    random.nextBytes(buffer)
    buffer
  }
}
