package su.knst.moneysaver.http.routers.user.registration

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{as, complete, entity, get, path, pathEnd, post}
import akka.http.scaladsl.server.Route
import com.google.inject.Inject
import su.knst.moneysaver.http.directives.Auth
import su.knst.moneysaver.http.routers.user.UsersDatabase
import su.knst.moneysaver.utils.G._
import su.knst.moneysaver.utils.{GsonMessage, HttpResult, ServerOptions}
import su.knst.moneysaver.utils.logger.DefaultLogger
import akka.http.scaladsl.server.Directives._

import java.util.UUID

class UserRegistrationRouter @Inject()
(
  db: UserRegistrationDatabase,
  auth: Auth,
  user: UsersDatabase,
  options: ServerOptions
) {
  protected val log: DefaultLogger = DefaultLogger("http", "registration")

  def getRegistrationData: Route = {
    (get & auth) { user => {
      complete(gson.toJson(db.getUserRegistrationData(user.id)))
    }
    }
  }

  def registration: Route = {
    if (options.registration)
      (post & entity(as[UserRegistrationArgs])) { args => {
        if (user.userExist(args.email)){
          complete(StatusCodes.BadRequest)
        }else {
          complete(gson.toJson(
            new UserRegistrationResult(user.registerUser(args.email, args.password))
          ))
        }
      }}
    else
      get {
        complete(StatusCodes.Forbidden)
      }
  }

  def route: Route = {
    path("getData") {
      getRegistrationData
    } ~ pathEnd {
      registration
    }
  }

  class UserRegistrationArgs(val email: String, val password: String) extends GsonMessage
  class UserRegistrationResult(val token: UUID) extends HttpResult
}
