package su.knst.moneysaver.http.routers.receipt

import scala.jdk.CollectionConverters._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{as, complete, entity, get, path, pathEnd, post}
import akka.http.scaladsl.server.Route
import com.google.inject.Inject
import scalaj.http.{Http, HttpOptions}
import su.knst.moneysaver.http.directives.Auth
import su.knst.moneysaver.objects.{Plan, Tag}
import su.knst.moneysaver.utils.G.gson
import su.knst.moneysaver.utils.logger.DefaultLogger
import su.knst.moneysaver.{http, utils}
import su.knst.moneysaver.utils.GsonMessage

import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64
import scala.collection.mutable

class ReceiptRouter @Inject()
(
  auth: Auth
) {
  protected val log: DefaultLogger = DefaultLogger("http", "receipt")

  def check: Route = {
    (get & auth & parameters("args".as[String])) { (user, args) =>
      val userToken = user.receiptToken
      val dargs = new String(Base64.getDecoder.decode(args))
      log.info(s"User ${user.id} requested QR info: '$dargs'")

      complete(
        Http("https://proverkacheka.com/api/v1/check/get")
          .postData("token=" + userToken + "&" + dargs)
          .option(HttpOptions.readTimeout(10000))
          .asString.body
      )
    }
  }

  def route: Route = {
    path("check") {
      check
    }
  }
}
