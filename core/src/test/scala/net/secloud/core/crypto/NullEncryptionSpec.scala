package net.secloud.core.crypto

import net.secloud.core.utils._
import org.specs2.mutable._

class NullEncryptionSpec extends Specification {
  "NullEncryption" should {
    "en- and decrypt" in {
      val nu = NullEncryption.generate(0)

      val plainIn = "Hello World".getBytes("UTF-8")
      val encrypted = StreamUtils.streamAsBytes(bs => nu.encrypt(bs)(es => StreamUtils.writeBytes(es, plainIn)))
      val plainOut = StreamUtils.bytesAsStream(encrypted)(bs => nu.decrypt(bs)(ds => StreamUtils.readBytes(ds)))

      plainIn === encrypted
      plainIn === plainOut
    }
  }
}
