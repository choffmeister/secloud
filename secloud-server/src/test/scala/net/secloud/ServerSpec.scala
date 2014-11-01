package net.secloud

import org.specs2.mutable._

class ServerSpec extends Specification {
  "Server" should {
    "run" in {
      val server = new Server()
      server.startup()
      server.shutdown()

      ok
    }
  }
}
