package de.choffmeister.secloud.commandline

import java.io.File
import org.rogach.scallop._
import de.choffmeister.secloud.core.Secloud
import de.choffmeister.secloud.core.Environment
import java.util.Date

object Application {
  def main(args: Array[String]): Unit = {
    val env = createEnvironment()

    execute(env, new CommandLineInterface(args))
  }
  
  def createEnvironment(): Environment = Environment(
    new File(System.getProperty("user.dir")),
    new File(System.getProperty("user.home")),
    new Date(System.currentTimeMillis)
  )

  def execute(env: Environment, cli: CommandLineInterface): Unit = cli.subcommand match {
    case Some(cli.init) =>
      println("init")
    case Some(cli.environment) =>
      println(s"Current directory: ${env.currentDirectory}")
      println(s"Home directory ${env.userDirectory}")
      println(s"Now: ${env.now}")
    case _ =>
      cli.printHelp()
  }

  class CommandLineInterface(val arguments: Seq[String]) extends ScallopConf(arguments) {
    version(s"${Secloud.name} v${Secloud.version} (c) ${Secloud.year} ${Secloud.copyright}")

    val init = new Subcommand("init")
    val environment = new Subcommand("environment")
  }
}
