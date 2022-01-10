package su.knst.moneysaver.utils.console.commands

import su.knst.moneysaver.utils.console.AbstractCommand

class NotFoundCommand extends AbstractCommand{
  override def apply(args: List[String]): Unit = {
    println("Command not found")
  }

  override def name: String = "404"

  override def aliases: List[String] = List.empty

  override def info: String = ""
}
