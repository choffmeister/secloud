package net.secloud.core

import java.io.File

import akka.actor._
import net.secloud.core.filewatcher.FileWatcherEvents
import net.secloud.core.utils.AggregatingActor

class RepositoryActor(val env: Environment, val conf: Config, val repo: Repository) extends Actor with ActorLogging {
  import FileWatcherEvents._

  val fileEventAggregator = context.actorOf(Props(AggregatingActor(self, 1000L)))

  def receive = {
    case e: FileWatcherEvent ⇒
      fileEventAggregator ! e

    case l: List[Any] if l.forall(_.isInstanceOf[FileWatcherEvent]) ⇒
      val events = l.map(_.asInstanceOf[FileWatcherEvent])
      if (events.exists(e ⇒ e.isInstanceOf[Error] || e == Overflow)) {
        log.warning("Got filewatcher error. Recommitting whole repository")
        commit(repo, Nil)
      } else {
        val hints = events.map(_.asInstanceOf[FileWatcherEventWithPath]).map(_.f)
        val hints2 = hints.filter(h ⇒ VirtualFile.fromFile(env.currentDirectory, h).segments.headOption != Some(".secloud"))
        log.info("Got filewatcher event. Committing with hints {}", hints2.map(_.toString).mkString(", "))
        if (hints.nonEmpty) commit(repo, hints2)
      }

    case x ⇒
      log.error("Unknown message {}", x)
  }

  def commit(repo: Repository, hints: List[File]): Unit = {
    val start = System.currentTimeMillis
    val id = repo.commit(hints.map(h ⇒ VirtualFile.fromFile(env.currentDirectory, h)))
    val end = System.currentTimeMillis
    log.info(s"Committed with new id ${id.hex} (${end - start} ms)")
  }
}
