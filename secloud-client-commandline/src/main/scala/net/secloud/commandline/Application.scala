package net.secloud.commandline

import java.io._
import akka.actor._
import net.secloud.core._
import net.secloud.core.filewatcher._
import net.secloud.core.objects._
import net.secloud.core.utils.AggregatingActor
import net.secloud.core.utils.StreamUtils._
import scala.language.reflectiveCalls

object Application {
  private lazy val log = org.slf4j.LoggerFactory.getLogger(getClass)
  private lazy val con = new StandardConsole()

  lazy val env = Environment()
  lazy val conf = Config()

  def main(args: Array[String]): Unit = {
    try {
      execute(new CommandLineInterface(args))
      System.exit(0)
    } catch {
      case e: Throwable ⇒
        log.error(e.getMessage, e)
        con.error(e.getMessage)
        System.exit(1)
    }
  }

  def execute(cli: CommandLineInterface): Unit = {
    cli.subcommand match {
      case Some(cli.init) ⇒
        val repo = Repository(env.currentDirectory, conf)
        con.info("Initializing new repository...")
        val commitId = repo.init()
        con.success("Done")
      case Some(cli.keygen) ⇒
        con.info("Generating RSA 2048-bit key...")
        KeyGenerator.generate(env, 2048, 128)
        con.success("Done")
      case Some(cli.commit) ⇒
        val repo = Repository(env.currentDirectory, conf)
        con.info("Committing current snapshot...")
        val commitId = repo.commit()
        con.success("Done")
      case Some(cli.history) ⇒
        val repo = Repository(env.currentDirectory, conf)
        val list = scala.collection.mutable.ListBuffer.empty[Commit]
        val queue = scala.collection.mutable.Queue.empty[Commit]
        queue.enqueue(repo.headCommit)

        while (queue.nonEmpty) {
          val curr = queue.dequeue()
          list += curr
          curr.parentIds.map(id ⇒ repo.database.readCommit(id, Right(conf.asymmetricKey))).foreach(c ⇒ queue.enqueue(c))
        }

        list.map(_.id.hex).foreach(println)
      case Some(cli.ls) ⇒
        val file = VirtualFile(cli.ls.path())
        val repo = Repository(env.currentDirectory, conf)
        val rfs = repo.fileSystem(repo.headCommit)
        val tree = rfs.tree(file)
        tree.entries.foreach(e ⇒ println(e.name))
      case Some(cli.cat) ⇒
        val file = VirtualFile(cli.ls.path())
        val repo = Repository(env.currentDirectory, conf)
        val rfs = repo.fileSystem(repo.headCommit)
        rfs.read(file)(cs ⇒ readLines(cs, l ⇒ con.stdout(l)))
      case Some(cli.tree) ⇒
        val repo = Repository(env.currentDirectory, conf)
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
      case Some(cli.watch) ⇒
        implicit val system = ActorSystem()
        val repo = Repository(env.currentDirectory, conf)
        val repoActor = system.actorOf(Props(new RepositoryActor(env, conf, repo)))
        System.out.println("Press a key to stop...")
        System.in.read()
      case Some(cli.environment) ⇒
        con.info(s"Current directory ${env.currentDirectory}")
        con.info(s"Home directory ${env.userDirectory}")
      case Some(cli.benchmark) ⇒
        Benchmark.fullBenchmark()
      case _ ⇒
        cli.printHelp()
    }
  }
}
