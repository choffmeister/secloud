package net.secloud

import akka.actor._

import scala.concurrent.duration._

object Server extends BootableApp[Server]

class Server extends Bootable {
  implicit val system = ActorSystem("secloud")
  implicit val executor = system.dispatcher

  def startup() = {
  }

  def shutdown() = {
    system.shutdown()
    system.awaitTermination(1.seconds)
  }
}
