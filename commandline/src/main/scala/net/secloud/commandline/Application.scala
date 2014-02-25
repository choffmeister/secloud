package net.secloud.commandline

import java.io._
import java.util.Date
import org.rogach.scallop._
import net.secloud.core._
import net.secloud.core.objects._
import net.secloud.core.crypto._
import scala.language.reflectiveCalls

object Application {
  def main(args: Array[String]): Unit = {
    val env = createEnvironment()

    try {
      execute(env, new CommandLineInterface(args))
      System.exit(0)
    } catch {
      case e: Throwable =>
        System.err.println("Error: " + e.getMessage)
        System.err.println("Type: " + e.getClass.getName)
        System.err.println("Stack trace:")
        e.getStackTrace.map("  " + _).foreach(System.err.println)
        System.exit(1)
    }
  }

  def execute(env: Environment, cli: CommandLineInterface): Unit = {
    cli.subcommand match {
      case Some(cli.init) =>
        val repo = openRepository(env)
        println("initializing...")
        println(repo.init())
      case Some(cli.keygen) =>
        println("generating RSA 2048-bit key...")
        KeyGenerator.generate(env, 2048, 128)
      case Some(cli.commit) =>
        val repo = openRepository(env)
        println("commiting...")
        println(repo.commit())
      case Some(cli.ls) =>
        val file = VirtualFile(cli.ls.path())
        val repo = openRepository(env)
        val tree = repo.traverse(file).asInstanceOf[Tree]
        tree.entries.foreach(e => println(e.name))
      case Some(cli.cat) =>
        val file = VirtualFile(cli.ls.path())
        val repo = openRepository(env)
        repo.read(file) { cs =>
          val reader = new BufferedReader(new InputStreamReader(cs))
          var done = false
          while (!done) {
            val line = Option(reader.readLine())
            if (line.isDefined) {
              println(line.get)
            } else done = true
          }
        }
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

  def openRepository(env: Environment): Repository = {
    val asymmetricKey = loadAsymmetricKey(env)
    val symmetricAlgorithm = AES
    val symmetricAlgorithmKeySize = 32
    val config = RepositoryConfig(asymmetricKey, symmetricAlgorithm, symmetricAlgorithmKeySize)
    Repository(env.currentDirectory, config)
  }

  def loadAsymmetricKey(env: Environment): AsymmetricAlgorithmInstance = {
    val f = new File(new File(env.userDirectory, ".secloud"), "rsa.key")
    try {
      val fs = new FileInputStream(f)
      try {
        RSA.loadFromPEM(fs)
      } finally {
        fs.close()
      }
    } catch {
      case e: Throwable => throw new Exception(s"Could not load RSA private key from ${f}", e)
    }
  }

  def createEnvironment(): Environment = Environment(
    new File(System.getProperty("user.dir")),
    new File(System.getProperty("user.home")),
    new Date(System.currentTimeMillis)
  )

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

    val environment = new Subcommand("environment")
    val benchmark = new Subcommand("benchmark")
  }
}
