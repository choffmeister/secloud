package net.secloud.commandline

import org.fusesource.jansi._

trait Console {
  def info(message: String): Unit
  def success(message: String): Unit
  def error(message: String): Unit

  def clear(): Unit
  def stdout(s: String): Unit
  def stderr(s: String): Unit
}

class StandardConsole extends Console {
  private lazy val ansi = new Ansi()
  AnsiConsole.systemInstall()

  def info(message: String): Unit = stdout("[info] " + message)
  def success(message: String): Unit = stdout(render("[@|green success|@] ") + message)
  def error(message: String): Unit = stdout(render("[@|red error|@] ") + message)

  def clear(): Unit = ansi.eraseScreen()
  def stdout(s: String): Unit = System.out.println(s)
  def stderr(s: String): Unit = System.err.println(s)

  private def render(s: String) = ansi.render(s)
}
