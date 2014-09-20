package net.secloud.core

import java.nio.file.LinkOption._
import java.nio.file.StandardWatchEventKinds._
import java.nio.file.WatchEvent.Kind
import java.nio.file._
import java.nio.file.attribute.BasicFileAttributes

import akka.actor.ActorRef
import com.sun.nio.file.SensitivityWatchEventModifier

import scala.collection.JavaConversions._

sealed trait FileWatcherEvent
case class CreatedEvent(p: Path) extends FileWatcherEvent
case class ModifiedEvent(p: Path) extends FileWatcherEvent
case class DeletedEvent(p: Path) extends FileWatcherEvent
case object OverflowError extends FileWatcherEvent
case class Error(err: Throwable) extends FileWatcherEvent

class FileWatcher(val path: Path, actorRef: ActorRef) extends Thread {
  private val watcher = FileSystems.getDefault.newWatchService()
  private var keys = Map.empty[WatchKey, Path]

  private def register(p: Path): Unit = {
    Files.walkFileTree(p, new SimpleFileVisitor[Path] {
      override def preVisitDirectory(p2: Path, attrs: BasicFileAttributes): FileVisitResult = {
        val eventTypes = Array[Kind[_]](ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
        val attrs = SensitivityWatchEventModifier.HIGH
        val key = p2.register(watcher, eventTypes, attrs)
        keys += key -> p2
        FileVisitResult.CONTINUE
      }
    })
  }

  override def run(): Unit = while (true) try {
    if (!keys.exists(_._2 == path)) register(path)
    val key = watcher.take
    keys.find(_._1 == key).map(_._2).foreach { dir ⇒
      for (ev ← key.pollEvents()) ev.kind match {
        case ENTRY_CREATE ⇒
          val event = ev.asInstanceOf[WatchEvent[Path]]
          val name = event.context
          val child = dir.resolve(name)
          if (Files.isDirectory(child, NOFOLLOW_LINKS)) register(child)
          actorRef ! CreatedEvent(child)
        case ENTRY_MODIFY ⇒
          val event = ev.asInstanceOf[WatchEvent[Path]]
          val name = event.context
          val child = dir.resolve(name)
          actorRef ! ModifiedEvent(child)
        case ENTRY_DELETE ⇒
          val event = ev.asInstanceOf[WatchEvent[Path]]
          val name = event.context
          val child = dir.resolve(name)
          actorRef ! DeletedEvent(child)
        case OVERFLOW ⇒
          actorRef ! OverflowError
        case x ⇒
          actorRef ! Error(new Exception(s"Unknown event ${x.name()}"))
      }
    }
    if (!key.reset()) keys -= key
  } catch {
    case t: Throwable ⇒ actorRef ! Error(t)
  }
}
