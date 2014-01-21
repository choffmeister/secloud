package net.secloud.commandline

import java.io.File
import java.util.Date
import org.rogach.scallop._
import net.secloud.core.Secloud
import net.secloud.core.Environment
import net.secloud.core.Benchmark

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
    case Some(cli.benchmark) =>
      Benchmark.fullBenchmark()
    case _ =>
      cli.printHelp()
  }

  class CommandLineInterface(val arguments: Seq[String]) extends ScallopConf(arguments) {
    version(s"${Secloud.name} v${Secloud.version} (c) ${Secloud.year} ${Secloud.copyright}")

    val init = new Subcommand("init")
    val environment = new Subcommand("environment")
    val benchmark = new Subcommand("benchmark")
  }
}
