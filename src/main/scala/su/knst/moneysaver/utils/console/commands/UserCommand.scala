package su.knst.moneysaver.utils.console.commands

import com.google.inject.{Inject, Singleton}
import org.mindrot.jbcrypt.BCrypt
import su.knst.moneysaver.http.routers.pushing.WebPushingDatabase
import su.knst.moneysaver.http.routers.user.UsersDatabase
import su.knst.moneysaver.objects.PushNotification
import su.knst.moneysaver.utils.config.MainConfig
import su.knst.moneysaver.utils.console.AbstractCommand

@Singleton
class UserCommand @Inject()
(
  config: MainConfig,
  webPushingDatabase: WebPushingDatabase,
  users: UsersDatabase
) extends AbstractCommand{
  private implicit val domain: String = config.server.saveStringIfNull("url", "https://ms.knst.su/")

  override def apply(args: List[String]): Unit = {

    args match {
      case Seq("register" | "reg", email, password) => println(email + "'s token: " + users.registerUser(email, password))

      case Seq("password" | "pass", email, password) =>
        try {
          users.changePasswordUser(users.getUser(email).id, password)
          println("password changed")
        }catch {
          case e: Exception => println("error:"); e.printStackTrace()
        }

      case Seq("send", email, text @ _*) =>
        val devices = webPushingDatabase.sendNotificationToUser(users.getUser(email).id, PushNotification.createDefault(
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
