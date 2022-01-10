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

import java.time.Instant
import scala.collection.mutable

class AccountsRouter @Inject()
(
  api: API,
  auth: Auth
) {

  def add: Route = {
    (post & auth) { user =>
      entity(as[NewAccountArgs]) { args => {
        api.newAccount(user.id, args.name)

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

  def setName: Route = {
    (post & auth) { user =>
      entity(as[SetNameAccountArgs]) { args => {
        val userAccounts : mutable.Buffer[Account] = api.getUserAccounts(user.id).asScala
        if (userAccounts.exists(a => a.id == args.id)) {
          api.setAccountName(args.id, args.name)
          complete(StatusCodes.OK)
        }else
          complete(StatusCodes.BadRequest)
      }
      }
    }
  }

  def route: Route = {
    path("add") {
      add
    } ~ path("setName") {
      setName
    } ~ pathEnd {
      main
    }
  }

  class SetNameAccountArgs(val id: Int, val name: String) extends GsonMessage
  class NewAccountArgs(val name: String) extends GsonMessage
}
