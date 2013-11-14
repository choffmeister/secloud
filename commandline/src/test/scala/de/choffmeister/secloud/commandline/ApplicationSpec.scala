package de.choffmeister.secloud.commandline

import org.specs2.mutable._
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import de.choffmeister.secloud.commandline.Application._

@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends Specification {
  "CommandLineInterface" should {
    "handle init command" in {
      val cli = new CommandLineInterface(Seq("init"))
      
      cli.subcommand === Some(cli.init)
    }
  }
}