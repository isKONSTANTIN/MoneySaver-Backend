package su.knst.moneysaver
package http

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.http.scaladsl.settings.RoutingSettings
import com.google.inject.Singleton
import ch.megard.akka.http.cors.scaladsl.CorsDirectives._
import com.sun.net.httpserver.HttpServer
import su.knst.moneysaver.exceptions.UserNotAuthorizedException
import su.knst.moneysaver.http.routers._
import su.knst.moneysaver.http.routers.accounts.AccountsRouter
import su.knst.moneysaver.http.routers.info.UserMainInfoRouter
import su.knst.moneysaver.http.routers.plans.PlansRouter
import su.knst.moneysaver.http.routers.tags.TagsRouter
import su.knst.moneysaver.http.routers.transactions.TransactionsRouter
import su.knst.moneysaver.http.routers.transactions.repeat.RepeatTransactionsRouter
import ch.megard.akka.http.cors.scaladsl.settings.CorsSettings
import su.knst.moneysaver.http.routers.receipt.ReceiptRouter
import su.knst.moneysaver.http.routers.user.UserRouter
import su.knst.moneysaver.services.ServiceCollector

import javax.inject.Inject
import scala.concurrent.Future

@Singleton
class HttpServer @Inject()
(
  implicit system: ActorSystem,
  helloWorldRouter: HelloWorldRouter,
  transactions: TransactionsRouter,
  repeatTransactions: RepeatTransactionsRouter,
  tags: TagsRouter,
  plans: PlansRouter,
  accounts: AccountsRouter,
  info: UserMainInfoRouter,
  collector: ServiceCollector,
  user: UserRouter,
  receipt: ReceiptRouter
){
  def routers : Route = {
    val exceptionHandler = ExceptionHandler {
      case _: UserNotAuthorizedException => complete(StatusCodes.Unauthorized)
      case e: Exception => {
        e.printStackTrace()
        complete(StatusCodes.InternalServerError, "Oops...")
      }
    }

    (cors(CorsSettings.defaultSettings) & handleExceptions(exceptionHandler)) {
      pathPrefix("api") {
        concat(
          pathPrefix("user") {
            user.route
          },

          pathPrefix("transactions") {
            pathPrefix("repeat") {
              repeatTransactions.route
            } ~ transactions.route
          },

          pathPrefix("tags") {
            tags.route
          },

          pathPrefix("plans") {
            plans.route
          },

          pathPrefix("accounts") {
            accounts.route
          },

          pathPrefix("info") {
            info.route
          },

          pathPrefix("receipt") {
            receipt.route
          },
        )
      }
    }
  }

  def start(): Future[Http.ServerBinding] = {
    Http().newServerAt("0.0.0.0", 8080).bind(routers)
  }
}
