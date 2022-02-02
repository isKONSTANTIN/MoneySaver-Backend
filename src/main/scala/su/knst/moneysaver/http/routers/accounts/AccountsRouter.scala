package su.knst.moneysaver
package http.routers.accounts

import http.directives.Auth
import utils.G.gson
import utils.{API, GsonMessage}
import utils.G._

import scala.jdk.CollectionConverters._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{as, complete, entity, get, path, pathEnd, post}
import akka.http.scaladsl.server.Route
import com.google.inject.Inject
import su.knst.moneysaver.objects.{Account, Plan, Tag}
import su.knst.moneysaver.utils.logger.DefaultLogger

import java.time.Instant
import scala.collection.mutable

class AccountsRouter @Inject()
(
  api: API,
  auth: Auth
) {
  protected val log: DefaultLogger = DefaultLogger("http", "accounts")

  def add: Route = {
    (post & auth) { user =>
      entity(as[NewAccountArgs]) { args => {
        val newId = api.newAccount(user.id, args.name)

        log.info(s"New account #$newId created")
        complete(StatusCodes.Created)
      }
      }
    }
  }

  def main: Route = {
    (get & auth) { user =>
      complete(gson.toJson(api.getUserAccounts(user.id)))
    }
  }

  def transfer: Route = {
    (post & auth) { user =>
      entity(as[TransferArgs]) { args => {
        val userAccounts = api.getUserAccounts(user.id).asScala
        if (!userAccounts.exists(_.id == args.from) || !userAccounts.exists(_.id == args.to) || args.amount <= 0){
          complete(StatusCodes.BadRequest)
        }else {
          api.accountTransfer(args.from, args.to, args.amount)

          log.info(s"New account transfer: ${args.from} -> ${args.to}: ${args.amount}")
          complete(StatusCodes.OK)
        }
      }
      }
    }
  }

  def setName: Route = {
    (post & auth) { user =>
      entity(as[SetNameAccountArgs]) { args => {
        if (api.userOwnedAccount(user.id, args.id)) {
          api.setAccountName(args.id, args.name)
          log.info(s"Account #${args.id} renamed to '${args.name}'")
          complete(StatusCodes.OK)
        }else
          complete(StatusCodes.Forbidden)
      }
      }
    }
  }

  def route: Route = {
    path("add") {
      add
    } ~ path("transfer"){
      transfer
    } ~ path("setName") {
      setName
    } ~ pathEnd {
      main
    }
  }

  class SetNameAccountArgs(val id: Int, val name: String) extends GsonMessage
  class TransferArgs(val from: Int, val to: Int, val amount: Double) extends GsonMessage
  class NewAccountArgs(val name: String) extends GsonMessage
}
