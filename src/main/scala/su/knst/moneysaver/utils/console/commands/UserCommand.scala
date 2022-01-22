package su.knst.moneysaver.utils.console.commands

import com.google.inject.{Inject, Singleton}
import org.mindrot.jbcrypt.BCrypt
import su.knst.moneysaver.objects.PushNotification
import su.knst.moneysaver.utils.API
import su.knst.moneysaver.utils.config.MainConfig
import su.knst.moneysaver.utils.console.AbstractCommand

@Singleton
class UserCommand @Inject()
(
  api: API,
  config: MainConfig
) extends AbstractCommand{
  private implicit val domain: String = config.server.saveStringIfNull("url", "https://ms.knst.su/")

  override def apply(args: List[String]): Unit = {

    args match {
      case Seq("register" | "reg", email, password) => println(email + "'s token: " + api.registerUser(email, password))

      case Seq("password" | "pass", email, password) =>
        try {
          api.changePasswordUser(api.getUser(email).id, password)
          println("password changed")
        }catch {
          case e: Exception => println("error:"); e.printStackTrace()
        }

      case Seq("send", email, text @ _*) =>
        val devices = api.sendNotificationToUser(api.getUser(email).id, PushNotification.createDefault(
          "Уведомление от администратора",
          text.mkString(" ")
        ))
        println(s"Sent to $devices devices")

      case _ =>
        println("user register <email> <password> - register user")
        println("user pass <email> <password> - change user password")
        println("user send <email> <text> - send push-notify to user")
    }
  }

  override def name: String = "user"

  override def aliases: List[String] = List("usr")

  override def info: String = "User utils"
}
