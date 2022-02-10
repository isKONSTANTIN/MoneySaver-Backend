package su.knst.moneysaver.http.routers.pushing

import scala.jdk.CollectionConverters._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{as, complete, entity, get, path, pathEnd, post}
import akka.http.scaladsl.server.Route
import com.google.inject.Inject
import scalaj.http.{Http, HttpOptions}
import su.knst.moneysaver.http.directives.Auth
import su.knst.moneysaver.{http, utils}
import su.knst.moneysaver.objects.{Plan, Tag}
import su.knst.moneysaver.utils.config.MainConfig
import su.knst.moneysaver.utils.logger.DefaultLogger
import su.knst.moneysaver.utils.GsonMessage
import utils.G._

import java.time.Instant
import java.util.Base64
import scala.collection.mutable

class WebPushingRouter @Inject()
(
  db: WebPushingDatabase,
  auth: Auth,
  config: MainConfig
) {
  protected val log: DefaultLogger = DefaultLogger("http", "pushing")

  def publicKey : Route = {
    (get & auth) { user =>
      complete(config.webPush.saveStringIfNull("publicKey", "XXXX"))
    }
  }

  def set: Route = {
    (post & auth) { user =>
      entity(as[SetNotificationDataArgs]) { args => {
        db.addUserNotificationData(user.id, args.endpoint, args.auth, args.p256dh)
        log.info(s"Added new notify device at user ${user.id}")
        complete(StatusCodes.Accepted)
      }
      }
    }
  }

  def route : Route = {
    path("publicKey") {
      publicKey
    } ~ path("setNotificationData"){
      set
    }
  }

  class SetNotificationDataArgs(val endpoint: String, val auth: String, val p256dh: String) extends GsonMessage
}
