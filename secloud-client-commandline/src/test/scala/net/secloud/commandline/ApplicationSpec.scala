package net.secloud.commandline

import org.specs2.mutable._
import net.secloud.commandline.Application._

class ApplicationSpec extends Specification {
  "Application" should {
    "create environment object" in {
      val env = Application.createEnvironment()

      ok
    }
  }

  "CommandLineInterface" should {
    "handle init command" in {
      val cli = new CommandLineInterface(Seq("init"))

      cli.subcommand === Some(cli.init)
    }

    "handle environment command" in {
      val cli = new CommandLineInterface(Seq("environment"))

      cli.subcommand === Some(cli.environment)
    }
  }
}
