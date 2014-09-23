package net.secloud.core

import java.io.File

import akka.actor._
import net.secloud.core.filewatcher._
import net.secloud.core.utils.AggregatingActor

class RepositoryActor(val env: Environment, val conf: Config, val repo: Repository) extends Actor with ActorLogging {
  import FileWatcherEvents._

  val watcher = FileWatcher.watch(env, env.currentDirectory, self)
  log.info("Started watching directory {}", env.currentDirectory)
  val fileEventAggregator = context.actorOf(Props(AggregatingActor(self, 1000L)))

  def receive = {
    case e: FileWatcherEvent ⇒
      fileEventAggregator ! e

    case l: List[Any] if l.forall(_.isInstanceOf[FileWatcherEvent]) ⇒
      val events = l.map(_.asInstanceOf[FileWatcherEvent])
      val errors = events.filter(_.isInstanceOf[Error]).map(_.asInstanceOf[Error])
      if (errors.nonEmpty) {
        errors.foreach(err ⇒ log.error(err.err, "Got file watcher error"))
        log.info("Recommitting whole repository")
        commit(repo, Nil)
      } else {
        val hints = events.map(_.asInstanceOf[FileWatcherEventWithPath]).map(e ⇒ e.f -> VirtualFile.fromFile(env.currentDirectory, e.f))
        val filtered = hints.filter(h ⇒ conf.ignore.forall(i ⇒ h._2.matches(i) == false))
        if (filtered.nonEmpty) {
          log.info("Committing with hints {}", filtered.map(_._1.toString).mkString(", "))
          commit(repo, filtered.map(_._1))
        }
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
