package net.secloud.core

import java.io._
import java.util.concurrent.TimeUnit

import akka.actor._
import akka.testkit._
import net.secloud.core.filewatcher._
import org.specs2.mutable._

import scala.concurrent.duration._

class TestActorSystem extends TestKit(ActorSystem()) with ImplicitSender with After {
  def after = system.shutdown()
}

class FileWatcherSpec extends Specification {
  val timeout = FiniteDuration(30, TimeUnit.SECONDS)
  new TestActorSystem()
  "FileWatcher" should {
    "emit file change events" in new TestActorSystem() {
      val temp = TempDirectory.createTemporaryDirectory("")
      val env = Environment.apply().copy(osName = "unknown")
      val watch = FileWatcher.watch(env, temp, self)

      within(timeout) {
        newfile(file(temp, "README.md"))
        fishForMessage() {
          case FileWatcherEvents.Created(f) ⇒ f == file(temp, "README.md")
          case _ ⇒ false
        }
      }

      within(timeout) {
        mkdirs(file(temp, "src", "main", "scala", "net", "secloud"))
        newfile(file(temp, "src", "main", "scala", "net", "secloud", "Application.scala"))
        fishForMessage() {
          case FileWatcherEvents.Created(f) ⇒ f == file(temp, "src")
          case _ ⇒ false
        }
      }

      within(timeout) {
        delete(file(temp, "src", "main", "scala", "net", "secloud", "Application.scala"))
        delete(file(temp, "src", "main", "scala", "net", "secloud"))
        delete(file(temp, "src", "main", "scala", "net"))
        delete(file(temp, "src", "main", "scala"))
        fishForMessage() {
          case FileWatcherEvents.Deleted(f) ⇒ f == file(temp, "src", "main", "scala")
          case _ ⇒ false
        }
      }
    }
  }

  def file(base: File, suffix: String*): File = file(base, suffix.toList)
  def file(base: File, suffix: List[String]): File = suffix match {
    case head :: tail ⇒ file(new File(base, head), tail)
    case _ ⇒ base
  }
  def mkdirs(f: File): Unit = f.mkdirs()
  def newfile(f: File): Unit = f.createNewFile()
  def delete(f: File): Unit = f.delete()
  def pause(delay: Long = 5000L) = Thread.sleep(delay)
}
