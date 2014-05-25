package net.secloud.commandline

import java.io._
import java.util.Date
import net.secloud.core._
import net.secloud.core.objects._
import net.secloud.core.crypto._
import scala.language.reflectiveCalls

object Application {
  private lazy val log = org.slf4j.LoggerFactory.getLogger(getClass)
  private lazy val con = new StandardConsole()

  def main(args: Array[String]): Unit = {
    val env = createEnvironment()

    try {
      execute(env, new CommandLineInterface(args))
      System.exit(0)
    } catch {
      case e: Throwable ⇒
        log.error(e.getMessage, e)
        con.error(e.getMessage)
        System.exit(1)
    }
  }

  def execute(env: Environment, cli: CommandLineInterface): Unit = {
    cli.subcommand match {
      case Some(cli.init) ⇒
        val repo = openRepository(env)
        con.info("Initializing new repository...")
        val commitId = repo.init()
        con.success("Done")
      case Some(cli.keygen) ⇒
        con.info("Generating RSA 2048-bit key...")
        KeyGenerator.generate(env, 2048, 128)
        con.success("Done")
      case Some(cli.commit) ⇒
        val repo = openRepository(env)
        con.info("Committing current snapshot...")
        val commitId = repo.commit()
        con.success("Done")
      case Some(cli.ls) ⇒
        val file = VirtualFile(cli.ls.path())
        val repo = openRepository(env)
        val rfs = repo.fileSystem(repo.headCommit)
        val tree = rfs.tree(file)
        tree.entries.foreach(e ⇒ println(e.name))
      case Some(cli.cat) ⇒
        val file = VirtualFile(cli.ls.path())
        val repo = openRepository(env)
        val rfs = repo.fileSystem(repo.headCommit)
        rfs.read(file) { cs ⇒
          val reader = new BufferedReader(new InputStreamReader(cs))
          var done = false
          while (!done) {
            val line = Option(reader.readLine())
            if (line.isDefined) {
              con.stdout(line.get)
            } else done = true
          }
        }
      case Some(cli.tree) ⇒
        val repo = openRepository(env)
        val rfs = repo.fileSystem(repo.headCommit)
        def asciiTreeLayer(layers: List[(Int, Int)]): String = {
          val pre = layers.take(layers.length - 1).map(l ⇒ if (l._1 < l._2 - 1) "|  " else "   ").mkString
          val last = if (layers.last._1 < layers.last._2 - 1) "├─ " else "└─ "
          pre + last
        }
        def traverse(file: VirtualFile, layers: List[(Int, Int)]): Unit = rfs.obj(file) match {
          case t: Tree ⇒
            con.stdout(s"${t.id.toString.substring(0, 7)} ${asciiTreeLayer(layers)}${file.name}")
            val children = rfs.children(file).toList.zipWithIndex
            children.foreach(c ⇒ traverse(c._1, layers ++ List((c._2, children.length))))
          case b: Blob ⇒
            con.stdout(s"${b.id.toString.substring(0, 7)} ${asciiTreeLayer(layers)}${file.name}")
          case _ ⇒ throw new Exception()
        }
        traverse(VirtualFile("/"), List((0, 1)))
      case Some(cli.environment) ⇒
        con.info(s"Current directory ${env.currentDirectory}")
        con.info(s"Home directory ${env.userDirectory}")
        con.info(s"Now ${env.now}")
      case Some(cli.benchmark) ⇒
        Benchmark.fullBenchmark()
      case _ ⇒
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
      case e: Throwable ⇒ throw new Exception(s"Could not load RSA private key from ${f}", e)
    }
  }

  def createEnvironment(): Environment = Environment(
    new File(System.getProperty("user.dir")),
    new File(System.getProperty("user.home")),
    new Date(System.currentTimeMillis))
}
