package tui

import java.util.Scanner

import akka.actor.{Actor, ActorLogging}
import msg.ClientMessage
import msg.ClientMessage.RegisterView

private[tui] object Tui {

  val cmdMap = Map(
    "exit" -> ShutdownCommand,
    "shutdown" -> ShutdownCommand
  )

}

class Tui extends Actor with ActorLogging {
  log.debug("Initialize")

  val mainControl = context.actorSelection("../control")
  mainControl ! RegisterView(self)

  new Thread(new Runnable {
    override def run(): Unit = {
      val scanner = new Scanner(System.in)
      while (true) {
        context.self ! ConsoleInput(scanner.nextLine)
      }
    }
  }).start()

  override def receive = {
    case ConsoleInput(input) =>parseInput(input)
    case msg => log.error("Unhandled message: " + msg)
  }

  private def parseInput(input: String): Unit = {
    log.debug("Parsing input: " + input)

    if(input == "help") {
      log.info("help: print this help")
      for((key, cmd) <- Tui.cmdMap) {
        log.info(s"$key: ${cmd.description}")
      }
      return
    }

    val args = input.split(" ")
    val cmd = Tui.cmdMap.get(args(0))

    if (cmd.isEmpty) {
      log.error(s"Unknown command '${args(0)}', type 'help' to print available commands")
      return
    }

    if (args.length - 1 != cmd.get.numberArgs) {
      log.error(s"Command '${args(0)}' requires ${cmd.get.numberArgs} arguments")
      return
    }

    try {
      val msg = cmd.get.parse(args)
      mainControl ! msg
    } catch {
      case e: NumberFormatException => log.error(s"Wrong argument format of command '${args(0)}'")
    }
  }

}




private[tui] case class ConsoleInput(input: String)

private[tui] object ShutdownCommand extends TextCommand {
  override val description = "Shutdown the application"
  override val numberArgs = 0
  override def parse(args: Array[String]) = ClientMessage.Shutdown
}

