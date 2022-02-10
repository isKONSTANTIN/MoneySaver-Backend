package su.knst.moneysaver
package http.routers.transactions.repeat

import http.directives.Auth
import utils.GsonMessage
import utils.G._

import scala.jdk.CollectionConverters._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, PredefinedFromEntityUnmarshallers}
import com.google.inject.Inject
import su.knst.moneysaver.http.routers.accounts.AccountsDatabase
import su.knst.moneysaver.http.routers.tags.TagsDatabase
import su.knst.moneysaver.http.routers.transactions.TransactionsDatabase
import su.knst.moneysaver.objects.{RepeatTransaction, Tag}
import su.knst.moneysaver.utils.logger.DefaultLogger

import java.time.Instant
import scala.collection.mutable
import scala.reflect.ClassTag

class RepeatTransactionsRouter @Inject()
(
  db: TransactionsDatabase,
  tags: TagsDatabase,
  accounts: AccountsDatabase,
  auth: Auth
) {
  protected val log: DefaultLogger = DefaultLogger("http", "repeat_transactions")

  def add: Route = {
    (post & auth) { user =>
      entity(as[NewRepeatTransactionArgs]) { args => {
        if (tags.userOwnedTag(user.id, args.tag) && accounts.userOwnedAccount(user.id, args.account)) {
          val newId = db.newRepeatTransaction(user.id, args.tag, args.delta, args.account, args.repeatArg, Instant.ofEpochSecond(args.startRepeat), args.repeatFunc, args.description)
          log.info(s"New repeat transaction #$newId added")
          complete(StatusCodes.Created)
        }else{
          complete(StatusCodes.Forbidden)
        }
      }
      }
    }
  }

  def remove: Route = {
    (post & auth) { user =>
      entity(as[RemoveRepeatTransactionArgs]) { args => {
        if (db.userOwnRepeatTransaction(user.id, args.id)){
          db.removeRepeatTransaction(args.id)
          log.info(s"Repeat transaction #${args.id} removed")
          complete(StatusCodes.OK)
        } else
          complete(StatusCodes.Forbidden)
      }
      }
    }
  }

  def edit: Route = {
    (post & auth) { user =>
      entity(as[EditRepeatTransactionArgs]) { args => {
        if (db.userOwnRepeatTransaction(user.id, args.id)){
          db.editRepeatTransaction(args.id, args.tag, args.delta, args.account, args.repeatArg, Instant.ofEpochSecond(args.lastRepeat), args.repeatFunc, args.description)
          complete(StatusCodes.OK)
        } else
          complete(StatusCodes.Forbidden)
      }
      }
    }
  }

  def main: Route = {
    (get & auth & parameters("offset".as[Int].optional, "count".as[Int].optional)) { (user, offset, count) =>
      complete(gson.toJson(db.getUserRepeatTransactions(user.id, offset.getOrElse(0), count.getOrElse(10))))
    }
  }

  def route: Route = {
    path("add") {
      add
    } ~ path("edit") {
      edit
    } ~ path("remove") {
      remove
    } ~ pathEnd {
      main
    }
  }

  class RemoveRepeatTransactionArgs(val id: Int) extends GsonMessage
  class EditRepeatTransactionArgs(val id: Int, val tag: Int, val delta: Double, val account: Int, val repeatArg: Int, val lastRepeat: Int, val repeatFunc: Int, val description: String) extends GsonMessage
  class NewRepeatTransactionArgs(val tag: Int, val delta: Double, val account: Int, val repeatArg: Int, val startRepeat: Int, val repeatFunc: Int, val description: String) extends GsonMessage
}