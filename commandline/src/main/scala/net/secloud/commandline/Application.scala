package net.secloud.commandline

import java.io.File
import java.io.FileInputStream
import java.util.Date
import org.rogach.scallop._
import net.secloud.core._
import net.secloud.core.objects._
import net.secloud.core.crypto._

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

  def execute(env: Environment, cli: CommandLineInterface): Unit = {
    val asymmetricKey = RSA.generate(1024)
    val symmetricAlgorithm = AES
    val symmetricAlgorithmKeySize = 32
    val config = RepositoryConfig(asymmetricKey, symmetricAlgorithm, symmetricAlgorithmKeySize)
    val repo = Repository(env.currentDirectory, config)

    cli.subcommand match {
      case Some(cli.init) =>
        repo.init()
        println("init")
      case Some(cli.commit) =>
        println("commit")
        val rootTreeId = repo.commit()
        println("root tree " + rootTreeId)
      case Some(cli.environment) =>
        println(s"Current directory: ${env.currentDirectory}")
        println(s"Home directory ${env.userDirectory}")
        println(s"Now: ${env.now}")
      case Some(cli.benchmark) =>
        Benchmark.fullBenchmark()
      case _ =>
        cli.printHelp()
    }
  }

  class CommandLineInterface(val arguments: Seq[String]) extends ScallopConf(arguments) {
    version(s"${Secloud.name} v${Secloud.version} (c) ${Secloud.year} ${Secloud.copyright}")

    val init = new Subcommand("init")
    val commit = new Subcommand("commit")
    val environment = new Subcommand("environment")
    val benchmark = new Subcommand("benchmark")
  }
}
