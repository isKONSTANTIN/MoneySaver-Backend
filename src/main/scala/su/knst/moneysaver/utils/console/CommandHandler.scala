package su.knst.moneysaver.utils.console

import com.google.inject.{Inject, Singleton}
import su.knst.moneysaver.utils.console.commands._

@Singleton
class CommandHandler @Inject()
(
  help: HelpCommand,
  notFound: NotFoundCommand,
  user: UserCommand
) {
  private val commands: Map[String, AbstractCommand] =
    List(help, user)
      .flatMap(c => (c.aliases :+ c.name).map(_ -> c)).toMap

  def apply(line: String): Unit = {
    if (line.trim.isEmpty)
      return

    val words = line.split(" ")

    commands.getOrElse(words.head, notFound)(words.tail.toList)
  }
}
