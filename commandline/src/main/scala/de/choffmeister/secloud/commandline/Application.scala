package de.choffmeister.secloud.commandline

import de.choffmeister.secloud.core.Secloud
import org.rogach.scallop._

object Application {
  def main(args: Array[String]): Unit = {
    execute(new CommandLineInterface(args))
  }

  def execute(cli: CommandLineInterface): Unit = cli.subcommand match {
    case Some(cli.init) =>
      println("init")
    case _ =>
      cli.printHelp()
  }

  class CommandLineInterface(val arguments: Seq[String]) extends ScallopConf(arguments) {
    version(s"${Secloud.name} v${Secloud.version} (c) ${Secloud.year} ${Secloud.copyright}")

    val init = new Subcommand("init")
  }
}
