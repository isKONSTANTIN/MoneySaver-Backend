package su.knst.moneysaver
package http.routers.transactions.repeat

import http.directives.Auth
import utils.{API, GsonMessage}
import utils.G._
import scala.jdk.CollectionConverters._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, PredefinedFromEntityUnmarshallers}
import com.google.inject.Inject
import su.knst.moneysaver.objects.{RepeatTransaction, Tag}

import java.time.Instant
import scala.collection.mutable
import scala.reflect.ClassTag

class RepeatTransactionsRouter @Inject()
(
  api: API,
  auth: Auth
) {

  def add: Route = {
    (post & auth) { user =>
      entity(as[NewRepeatTransactionArgs]) { args => {
        if (api.userOwnedTag(user.id, args.tag) && api.userOwnedAccount(user.id, args.account)) {
          api.newRepeatTransaction(user.id, args.tag, args.delta, args.account, args.repeatArg, Instant.ofEpochSecond(args.startRepeat), args.repeatFunc, args.description)
          complete(StatusCodes.Created)
        }else{
          complete(StatusCodes.Forbidden)
        }

        complete(StatusCodes.Created)
      }
      }
    }
  }

  def remove: Route = {
    (post & auth) { user =>
      entity(as[RemoveRepeatTransactionArgs]) { args => {
        if (api.userOwnRepeatTransaction(user.id, args.id)){
          api.removeRepeatTransaction(args.id)
          complete(StatusCodes.OK)
        } else
          complete(StatusCodes.BadRequest)
      }
      }
    }
  }

  def edit: Route = {
    (post & auth) { user =>
      entity(as[EditRepeatTransactionArgs]) { args => {
        if (api.userOwnRepeatTransaction(user.id, args.id)){
          api.editRepeatTransaction(args.id, args.tag, args.delta, args.account, args.repeatArg, Instant.ofEpochSecond(args.lastRepeat), args.repeatFunc, args.description)
          complete(StatusCodes.OK)
        } else
          complete(StatusCodes.BadRequest)
      }
      }
    }
  }

  def main: Route = {
    (get & auth & parameters("offset".as[Int].optional, "count".as[Int].optional)) { (user, offset, count) =>
      complete(gson.toJson(api.getUserRepeatTransactions(user.id, offset.getOrElse(0), count.getOrElse(10))))
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