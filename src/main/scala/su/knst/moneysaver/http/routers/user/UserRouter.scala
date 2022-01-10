package su.knst.moneysaver
package http.routers.user

import http.directives.Auth
import utils.{API, GsonMessage}
import utils.G.gson
import utils.{API, GsonMessage}
import utils.G._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, PredefinedFromEntityUnmarshallers}
import com.google.inject.Inject
import su.knst.moneysaver.objects.User

import java.time.Instant
import java.util.UUID
import scala.reflect.ClassTag

class UserRouter @Inject()
(
  api: API,
  auth: Auth
){

  def route: Route = {
    path("auth") {
      (post & entity(as[AuthArgs])) { args => {
        val user : User = api.authUser(args.email, args.password)

        complete(gson.toJson(new AuthResult(user.token, user.email, user.receiptToken)))
      }
      }
    } ~ path("updateReceiptToken") {
      (post & auth & entity(as[UpdateReceiptArgs])) { (user, receipt) => {
        api.updateUserReceiptToken(user.id, receipt.receipt)
        complete(StatusCodes.OK)
      }
      }
    } ~ pathEnd {
      (get & auth) { user => {
        complete(gson.toJson(new AuthResult(user.token, user.email, user.receiptToken)))
      }
      }
    }
  }

  class AuthResult(val token: UUID, val email: String, val receiptToken: String)
  class UpdateReceiptArgs(val receipt: String) extends GsonMessage
  class AuthArgs(val email: String, val password: String) extends GsonMessage
}
