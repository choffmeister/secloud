package net.secloud.commandline

import net.secloud.core.Secloud
import org.rogach.scallop._

class CommandLineInterface(val arguments: Seq[String]) extends ScallopConf(arguments) {
  version(s"${Secloud.name} v${Secloud.version} (c) ${Secloud.year} ${Secloud.copyright}")

  val init = new Subcommand("init")
  val keygen = new Subcommand("keygen")
  val commit = new Subcommand("commit")

  val ls = new Subcommand("ls") {
    val path = trailArg[String]("the path")
  }
  val cat = new Subcommand("cat") {
    val path = trailArg[String]("the path")
  }
  val tree = new Subcommand("tree")

  val environment = new Subcommand("environment")
  val benchmark = new Subcommand("benchmark")
}
