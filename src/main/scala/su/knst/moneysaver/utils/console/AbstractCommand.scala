package su.knst.moneysaver.utils.console

abstract class AbstractCommand {

  def apply(args: List[String]): Unit
  def name: String
  def aliases: List[String]
  def info: String
}
