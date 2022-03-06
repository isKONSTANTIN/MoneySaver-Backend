package su.knst.moneysaver
package http.routers.user

import akka.actor.ActorSystem
import http.directives.Auth
import utils.{GsonMessage, HttpResult, StringValidator, StringValidatorSettings}
import StringValidator._
import utils.G.gson
import utils.G._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, PredefinedFromEntityUnmarshallers}
import com.google.inject.Inject
import com.wanari.webpush.{PushService, Subscription, Utils}
import org.mindrot.jbcrypt.BCrypt
import su.knst.moneysaver.exceptions.{UserNotAuthorizedException, WrongPasswordException}
import su.knst.moneysaver.objects.{AuthedUser, PushNotification, User, UserNotificationData}
import su.knst.moneysaver.utils.logger.DefaultLogger

import java.security.interfaces.{ECPrivateKey, ECPublicKey}
import java.time.Instant
import java.util.UUID
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.reflect.ClassTag

class UserRouter @Inject()
(
  db: UsersDatabase,
  auth: Auth
){
  protected val log: DefaultLogger = DefaultLogger("http", "user")
  protected implicit val validSettings: StringValidatorSettings = StringValidator.settings(1, 1024, true)

  def authR: Route = {
    (post & entity(as[AuthArgs])) { args => {
      throwInvalid(args.email, args.password)

      val user : AuthedUser = db.authUser(args.email, args.password)

      complete(gson.toJson(user))
    }
    }
  }

  def updateReceiptToken(): Route = {
    (post & auth & entity(as[UpdateReceiptArgs])) { (user, receipt) => {
      throwInvalid(receipt.receipt)

      db.updateUserReceiptToken(user.id, receipt.receipt)
      auth.invalidateUser(user.id)
      log.info(s"Receipt token by ${user.id} updated")
      complete(StatusCodes.OK)
    }
    }
  }

  def changePassword: Route = {
    (post & auth & entity(as[UpdatePasswordArgs])) { (user, args) => {
      throwInvalid(args.oldPassword, args.newPassword)

      if (!BCrypt.checkpw(args.oldPassword, db.getUser(user.email).password))
        throw new WrongPasswordException

      db.changePasswordUser(user.id, args.newPassword)

      complete(StatusCodes.OK)
    }
    }
  }

  def deactivateSession: Route = {
    (post & auth & entity(as[DeactivateSessionArgs])) { (user, args) => {
      if (db.getUser(args.session).id != user.id)
        throw new UserNotAuthorizedException

      db.deactivateUserSession(args.session)
      complete(StatusCodes.OK)
    }
    }
  }

  def getSessions: Route = {
    (get & auth) { user => {
      complete(gson.toJson(db.getUserActiveSessions(user.id)))
    }
    }
  }

  def main: Route = {
    (get & auth) { user => {
      complete(gson.toJson(user))
    }
    }
  }

  def route: Route = {
    path("auth") {
      authR
    } ~ path("updateReceiptToken") {
      updateReceiptToken()
    } ~ path("changePassword") {
      changePassword
    } ~ path("getSessions") {
      getSessions
    }  ~ path("deactivateSession") {
      deactivateSession
    } ~ pathEnd {
      main
    }
  }

  class AuthResult(val token: UUID, val email: String, val receiptToken: String) extends HttpResult
  class UpdateReceiptArgs(val receipt: String) extends GsonMessage
  class UpdatePasswordArgs(val oldPassword: String, val newPassword: String) extends GsonMessage
  class DeactivateSessionArgs(val session: UUID) extends GsonMessage
  class AuthArgs(val email: String, val password: String) extends GsonMessage
}
