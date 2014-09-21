package net.secloud.core.utils

import java.util.concurrent.TimeUnit

import akka.actor._
import net.secloud.core.TestActorSystem
import org.specs2.mutable.Specification

import scala.concurrent.duration.FiniteDuration

class AggregatingActorSpec extends Specification {
  "AggregatingActor" should {
    "work" in new TestActorSystem() {
      val agg = system.actorOf(Props(new AggregatingActor(self, secs(1))))

      within(secs(2)) {
        for (i ← 1 to 10) self ! s"Message $i"
        for (i ← 1 to 10) expectMsg(s"Message $i")
      }

      within(secs(2)) {
        for (i ← 1 to 10) agg ! s"Message $i"
        expectMsg((1 to 10).map(i ⇒ s"Message $i").toList)
      }
    }
  }

  def secs(i: Int) = FiniteDuration(i, TimeUnit.SECONDS)
}
