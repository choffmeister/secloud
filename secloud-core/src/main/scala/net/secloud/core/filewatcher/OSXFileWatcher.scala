package net.secloud.core.filewatcher

import java.io.File
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.{ FileVisitResult, Files, Path, Paths, SimpleFileVisitor }

import akka.actor.ActorRef
import com.barbarysoftware.watchservice.StandardWatchEventKind._
import com.barbarysoftware.watchservice.WatchEvent.Kind
import com.barbarysoftware.watchservice._
import com.sun.nio.file.SensitivityWatchEventModifier

import scala.collection.JavaConversions._

class OSXFileWatcher(val file: File, actorRef: ActorRef) extends FileWatcher {
  private val watcher = WatchService.newWatchService()
  private var keys = Map.empty[WatchKey, Path]
  private var _running = false
  def running = _running

  private def register(p: Path): Unit = {
    Files.walkFileTree(p, new SimpleFileVisitor[Path] {
      override def preVisitDirectory(p2: Path, attrs: BasicFileAttributes): FileVisitResult = {
        val eventTypes = Array[Kind[_]](ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)
        val attrs = SensitivityWatchEventModifier.HIGH
        val key = new WatchableFile(p2.toFile).register(watcher, eventTypes)
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
          val event = ev.asInstanceOf[WatchEvent[File]]
          val name = event.context
          val child = dir.resolve(Paths.get(name.toString))
          if (Files.isDirectory(child, NOFOLLOW_LINKS)) register(child)
          actorRef ! FileWatcherEvents.Created(child.toFile)
        case ENTRY_MODIFY ⇒
          val event = ev.asInstanceOf[WatchEvent[File]]
          val name = event.context
          val child = dir.resolve(Paths.get(name.toString))
          actorRef ! FileWatcherEvents.Modified(child.toFile)
        case ENTRY_DELETE ⇒
          val event = ev.asInstanceOf[WatchEvent[File]]
          val name = event.context
          val child = dir.resolve(Paths.get(name.toString))
          actorRef ! FileWatcherEvents.Deleted(child.toFile)
        case OVERFLOW ⇒
          actorRef ! FileWatcherEvents.Error(new Exception(s"Underlying file watcher notified an overflow"))
        case x ⇒
          actorRef ! FileWatcherEvents.Error(new Exception(s"Unknown event ${x.name()}"))
      }
    }
    if (!key.reset()) keys -= key
  } catch {
    case t: Throwable ⇒ actorRef ! FileWatcherEvents.Error(t)
  }
}
