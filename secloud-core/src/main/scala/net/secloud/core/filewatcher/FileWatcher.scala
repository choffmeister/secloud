package net.secloud.core.filewatcher

import java.io.File
import java.nio.file.LinkOption._
import java.nio.file.StandardWatchEventKinds._
import java.nio.file.WatchEvent.Kind
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes

import akka.actor._
import com.sun.nio.file.SensitivityWatchEventModifier
import net.secloud.core.Environment

import scala.collection.JavaConversions._
import scala.concurrent.duration.FiniteDuration

object FileWatcherEvents {
  sealed trait FileWatcherEvent
  case class Created(f: File) extends FileWatcherEvent
  case class Modified(f: File) extends FileWatcherEvent
  case class Deleted(f: File) extends FileWatcherEvent
  case object Overflow extends FileWatcherEvent
  case class Error(err: Throwable) extends FileWatcherEvent
}

trait FileWatcher extends Thread {
  def running: Boolean
}

class DefaultFileWatcher(val file: File, actorRef: ActorRef) extends FileWatcher {
  private val watcher = FileSystems.getDefault.newWatchService()
  private var keys = Map.empty[WatchKey, Path]
  private var _running = false
  def running = _running

  private def register(p: Path): Unit = {
    Files.walkFileTree(p, new SimpleFileVisitor[Path] {
      override def preVisitDirectory(p2: Path, attrs: BasicFileAttributes): FileVisitResult = {
        val eventTypes = Array[Kind[_]](ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
        val attrs = SensitivityWatchEventModifier.MEDIUM
        val key = p2.register(watcher, eventTypes, attrs)
        keys += key -> p2
        FileVisitResult.CONTINUE
      }
    })
  }

  override def run(): Unit = while (true) try {
    val path = Paths.get(file.toString)
    if (!keys.exists(_._2 == path)) register(path)
    _running = true
    val key = watcher.take
    keys.find(_._1 == key).map(_._2).foreach { dir ⇒
      for (ev ← key.pollEvents()) ev.kind match {
        case ENTRY_CREATE ⇒
          val event = ev.asInstanceOf[WatchEvent[Path]]
          val name = event.context
          val child = dir.resolve(name)
          if (Files.isDirectory(child, NOFOLLOW_LINKS)) register(child)
          actorRef ! FileWatcherEvents.Created(child.toFile)
        case ENTRY_MODIFY ⇒
          val event = ev.asInstanceOf[WatchEvent[Path]]
          val name = event.context
          val child = dir.resolve(name)
          actorRef ! FileWatcherEvents.Modified(child.toFile)
        case ENTRY_DELETE ⇒
          val event = ev.asInstanceOf[WatchEvent[Path]]
          val name = event.context
          val child = dir.resolve(name)
          actorRef ! FileWatcherEvents.Deleted(child.toFile)
        case OVERFLOW ⇒
          actorRef ! FileWatcherEvents.Overflow
        case x ⇒
          actorRef ! FileWatcherEvents.Error(new Exception(s"Unknown event ${x.name()}"))
      }
    }
    if (!key.reset()) keys -= key
  } catch {
    case t: Throwable ⇒ actorRef ! FileWatcherEvents.Error(t)
  }
}

object FileWatcher {
  def watch(env: Environment, file: File, actorRef: ActorRef): FileWatcher = {
    val watcher = env.osName match {
      // TODO: fix
      // case "Mac OS X" ⇒ new OSXFileWatcher(file, actorRef)
      case _ ⇒ new DefaultFileWatcher(file, actorRef)
    }
    watcher.start()
    blockUntil(watcher.running)
    watcher
  }

  private def blockUntil(cond: ⇒ Boolean): Unit = while (!cond) Thread.sleep(100L)
}
