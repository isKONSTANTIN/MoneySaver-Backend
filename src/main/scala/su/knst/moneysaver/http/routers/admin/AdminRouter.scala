package su.knst.moneysaver.http.routers.admin

import su.knst.moneysaver.{http, utils}
import http.directives.Auth
import utils.G.gson
import utils.GsonMessage
import utils.G._

import scala.jdk.CollectionConverters._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import com.google.inject.Inject
import su.knst.moneysaver.utils.logger.DefaultLogger
import su.knst.moneysaver.http.directives.AdminAuth
import su.knst.moneysaver.http.routers.pushing.WebPushingDatabase
import su.knst.moneysaver.http.routers.user.UsersDatabase
import su.knst.moneysaver.objects.PushNotification
import su.knst.moneysaver.utils.config.MainConfig

class AdminRouter @Inject()
(
  db: AdminDatabase,
  auth: AdminAuth,
  config: MainConfig,
  webPushingDatabase: WebPushingDatabase,
  users: UsersDatabase
) {
  protected val log: DefaultLogger = DefaultLogger("http", "admin")
  private implicit val domain: String = config.server.saveStringIfNull("url", "https://ms.knst.su/")

  def registerUser: Route = {
    (post & auth) { _ =>
      entity(as[RegisterUserArgs]) { args => {
        users.registerUser(args.email, args.password)
        log.info(s"User ${args.email} registered")
        complete(StatusCodes.Created)
      }
      }
    }
  }

  def changeUserPassword: Route = {
    (post & auth) { _ =>
      entity(as[ChangeUserPasswordArgs]) { args => {
        users.changePasswordUser(users.getUser(args.email).id, args.password)
        log.info(s"Password ${args.email} changed")
        complete(StatusCodes.OK)
      }
      }
    }
  }

  def sendNotificationToUser: Route = {
    (post & auth) { _ =>
      entity(as[NotificationArgs]) { args => {
        val devices = webPushingDatabase.sendNotificationToUser(
          users.getUser(args.email).id,
          PushNotification.createDefault(args.title, args.text)
        )

        complete(gson.toJson(new NotificationResult(devices)))
      }
      }
    }
  }

  def checkAccess: Route = {
    (get & auth) { _ =>
      complete(StatusCodes.OK)
    }
  }

  def getUsers: Route = {
    (get & auth & parameters("offset".as[Int].optional, "count".as[Int].optional)) { (_, offset, count) =>
      complete(gson.toJson(db.getUsers(offset.getOrElse(0), count.getOrElse(10))))
    }
  }

  def route: Route = {
    path("registerUser") {
      registerUser
    } ~ path("changeUserPassword") {
      changeUserPassword
    } ~ path("sendNotificationToUser") {
      sendNotificationToUser
    } ~ path("getUsers"){
      getUsers
    } ~ pathEnd {
      checkAccess
    }
  }
  class NotificationArgs(val email: String, val title: String, val text: String) extends GsonMessage
  class ChangeUserPasswordArgs(val email: String, val password: String) extends GsonMessage
  class RegisterUserArgs(val email: String, val password: String) extends GsonMessage

  class NotificationResult(val devices: Int)
}
