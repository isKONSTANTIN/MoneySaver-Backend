package su.knst.moneysaver.utils.console.commands

import com.google.inject.{Inject, Provider, Singleton}
import su.knst.moneysaver.utils.console.{AbstractCommand, CommandHandler}

@Singleton
class HelpCommand @Inject() extends AbstractCommand{
  override def apply(args: List[String]): Unit = {
    println("help - show commands \n" +
      "user - user utils")
  }

  override def name: String = "help"

  override def aliases: List[String] = List("?", "hello")

  override def info: String = "Help command. Just say 'hello'!"
}
