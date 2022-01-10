package su.knst.moneysaver.utils.console.commands

import com.google.inject.{Inject, Singleton}
import org.mindrot.jbcrypt.BCrypt
import su.knst.moneysaver.utils.API
import su.knst.moneysaver.utils.console.AbstractCommand

@Singleton
class UserCommand @Inject()
(
  api: API
) extends AbstractCommand{
  override def apply(args: List[String]): Unit = {

    args match {
      case Seq("register" | "reg", email, password) => println(email + "'s token: " + api.registerUser(email, password))
      case Seq("password" | "pass", email, password) => {
        try {
          api.changePasswordUser(api.getUser(email).id, password)
          println("password changed")
        }catch {
          case e: Exception => println("error:"); e.printStackTrace()
        }
      }
      case _ =>
        println("user register <email> <password> - register user")
        println("user pass <email> <password> - change user password")
    }
  }

  override def name: String = "user"

  override def aliases: List[String] = List("usr")

  override def info: String = "User utils"
}
