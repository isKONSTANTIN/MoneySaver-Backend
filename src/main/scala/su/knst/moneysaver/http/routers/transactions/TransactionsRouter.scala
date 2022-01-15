package su.knst.moneysaver
package http.routers.transactions

import http.directives.Auth
import utils.{API, GsonMessage}
import utils.G._
import akka.http.scaladsl.model.{HttpMessage, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, PredefinedFromEntityUnmarshallers}
import com.google.inject.Inject

import java.time.Instant
import scala.reflect.ClassTag

class TransactionsRouter @Inject()
(
  api: API,
  auth: Auth
) {

  def add: Route = {
    (post & auth) { user =>
      entity(as[NewTransactionArgs]) { args => {
        if (api.userOwnedTag(user.id, args.tag) && api.userOwnedAccount(user.id, args.account)) {
          api.newTransaction(user.id, args.delta, args.tag, Instant.ofEpochSecond(args.date), args.account, args.description)
          complete(StatusCodes.Created)
        }else{
          complete(StatusCodes.Forbidden)
        }
      }
      }
    }
  }

  def month: Route = {
    (get & auth & parameters("offset".as[Int].optional, "count".as[Int].optional)) { (user, offset, count) =>
      complete(gson.toJson(api.getUserTransactionsPerMonth(user.id, offset.getOrElse(0), count.getOrElse(10))))
    }
  }

  def cancel: Route = {
    (post & auth) { user =>
      entity(as[CancelTransactionArgs]) { args => {
        if (api.userOwnedTransaction(user.id, args.id)){
          api.cancelTransaction(args.id)
          complete(StatusCodes.Created)
        }else {
          complete(StatusCodes.Forbidden)
        }
      }
      }
    }
  }

  def main: Route = {
    (get & auth & parameters("offset".as[Int].optional, "count".as[Int].optional)) { (user, offset, count) =>
      complete(gson.toJson(api.getUserTransactions(user.id, offset.getOrElse(0), count.getOrElse(10))))
    }
  }

  def route: Route = {
    path("add") {
      add
    } ~ path("cancel") {
      cancel
    } ~ path("month") {
      month
    } ~ pathEnd {
      main
    }
  }

  class NewTransactionArgs(val delta: Double, val tag: Int, val date: Int, val account: Int, val description: String) extends GsonMessage
  class CancelTransactionArgs(val id: Int) extends GsonMessage
}
