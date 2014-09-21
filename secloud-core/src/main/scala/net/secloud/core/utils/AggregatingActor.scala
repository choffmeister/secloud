package net.secloud.core.utils

import java.util.concurrent.TimeUnit

import akka.actor._

import scala.concurrent.duration.FiniteDuration

sealed trait AggregatingState
case object EmptyState extends AggregatingState
case object FilledState extends AggregatingState
case class AggregatingData(messages: List[Any])

class AggregatingActor(proxyFor: ActorRef, timeout: FiniteDuration) extends FSM[AggregatingState, AggregatingData] {
  startWith(EmptyState, AggregatingData(Nil))

  when(EmptyState) {
    case Event(x, data) ⇒
      goto(FilledState) using data.copy(messages = x :: data.messages)
  }

  when(FilledState, stateTimeout = timeout) {
    case Event(StateTimeout, data) ⇒
      proxyFor ! data.messages.reverse
      goto(EmptyState) using AggregatingData(Nil)
    case Event(x, data) ⇒
      goto(FilledState) using data.copy(messages = x :: data.messages)
  }
}

object AggregatingActor {
  def apply(proxyFor: ActorRef, timeoutMillis: Long): AggregatingActor = new AggregatingActor(proxyFor, FiniteDuration(timeoutMillis, TimeUnit.MILLISECONDS))
}
