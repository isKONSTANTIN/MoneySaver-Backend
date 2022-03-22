package su.knst.moneysaver
package http.routers.transactions

import http.directives.Auth
import utils.{GsonMessage, StringValidator, StringValidatorSettings}
import utils.G._
import akka.http.scaladsl.model.{HttpMessage, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, PredefinedFromEntityUnmarshallers}
import com.google.inject.Inject
import su.knst.moneysaver.http.routers.accounts.AccountsDatabase
import su.knst.moneysaver.http.routers.tags.TagsDatabase
import su.knst.moneysaver.utils.StringValidator.throwInvalid
import su.knst.moneysaver.utils.logger.DefaultLogger

import java.time.Instant
import scala.reflect.ClassTag

class TransactionsRouter @Inject()
(
  db: TransactionsDatabase,
  tags: TagsDatabase,
  accounts: AccountsDatabase,
  auth: Auth
) {
  protected val log: DefaultLogger = DefaultLogger("http", "transactions")
  protected implicit val validSettings: StringValidatorSettings = StringValidator.settings(0, 1024, true)

  def add: Route = {
    (post & auth) { user =>
      entity(as[NewTransactionArgs]) { args => {
        throwInvalid(args.description)

        if (tags.userOwnedTag(user.id, args.tag) && accounts.userOwnedAccount(user.id, args.account)) {
          val newId = db.newTransaction(user.id, args.delta, args.tag, Instant.ofEpochSecond(args.date), args.account, args.description)

          log.info(s"New transaction #$newId added")
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
      complete(gson.toJson(db.getUserTransactionsPerMonth(user.id, offset.getOrElse(0), count.getOrElse(10))))
    }
  }

  def edit: Route = {
    (post & auth) { user =>
      entity(as[EditTransactionArgs]) { args => {
        throwInvalid(args.description)

        if (db.userOwnedTransaction(user.id, args.id) && tags.userOwnedTag(user.id, args.tag) && accounts.userOwnedAccount(user.id, args.account)){
          db.editTransaction(args.id, args.delta, args.tag, Instant.ofEpochSecond(args.date), args.account, args.description)
          log.info(s"Transaction #${args.id} edited")
          complete(StatusCodes.OK)
        }else {
          complete(StatusCodes.Forbidden)
        }
      }
      }
    }
  }

  def cancel: Route = {
    (post & auth) { user =>
      entity(as[CancelTransactionArgs]) { args => {
        if (db.userOwnedTransaction(user.id, args.id)){
          db.cancelTransaction(args.id)
          log.info(s"Transaction #${args.id} canceled")
          complete(StatusCodes.OK)
        }else {
          complete(StatusCodes.Forbidden)
        }
      }
      }
    }
  }

  def main: Route = {
    (get & auth & parameters("offset".as[Int].optional, "count".as[Int].optional)) { (user, offset, count) =>
      complete(gson.toJson(db.getUserTransactions(user.id, offset.getOrElse(0), count.getOrElse(10))))
    }
  }

  def route: Route = {
    path("add") {
      add
    } ~ path("edit") {
      edit
    } ~ path("cancel") {
      cancel
    } ~ path("month") {
      month
    } ~ pathEnd {
      main
    }
  }

  class NewTransactionArgs(val delta: Double, val tag: Int, val date: Int, val account: Int, val description: String) extends GsonMessage
  class EditTransactionArgs(val id: Int, val delta: Double, val tag: Int, val date: Int, val account: Int, val description: String) extends GsonMessage
  class CancelTransactionArgs(val id: Int) extends GsonMessage
}
