package su.knst.moneysaver
package http.routers.plans

import http.directives.Auth
import utils.G.gson
import utils.{GsonMessage, StringValidator, StringValidatorSettings}
import utils.G._

import scala.jdk.CollectionConverters._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{as, complete, entity, get, path, pathEnd, post}
import akka.http.scaladsl.server.Route
import com.google.inject.Inject
import su.knst.moneysaver.http.routers.accounts.AccountsDatabase
import su.knst.moneysaver.http.routers.tags.TagsDatabase
import su.knst.moneysaver.objects.{Plan, Tag}
import su.knst.moneysaver.utils.StringValidator.throwInvalid
import su.knst.moneysaver.utils.logger.DefaultLogger

import java.time.Instant
import scala.collection.mutable

class PlansRouter @Inject()
(
  db: PlansDatabase,
  tags: TagsDatabase,
  accounts: AccountsDatabase,
  auth: Auth
) {
  protected val log: DefaultLogger = DefaultLogger("http", "plans")
  protected implicit val validSettings: StringValidatorSettings = StringValidator.settings(1, 1024, true)

  def add: Route = {
    (post & auth) { user =>
      entity(as[NewPlanArgs]) { args => {
        throwInvalid(args.description)

        if (tags.userOwnedTag(user.id, args.tag) && accounts.userOwnedAccount(user.id, args.account)) {
          val newId = db.newPlan(user.id, args.delta, args.tag, Instant.ofEpochSecond(args.date), args.account, args.description, 0)
          log.info(s"New plan #$newId created")
          complete(StatusCodes.Created)
        }else{
          complete(StatusCodes.Forbidden)
        }
      }
      }
    }
  }

  def getAll: Route = {
    (get & auth & parameters("offset".as[Int].optional, "count".as[Int].optional)) { (user, offset, count) =>
      complete(gson.toJson(db.getUserPlans(user.id, offset.getOrElse(0), count.getOrElse(0))))
    }
  }

  def completePlan: Route = {
    (post & auth) { user =>
      entity(as[CompletePlanArgs]) { args => {
        if (db.userOwnedPlan(user.id, args.id)) {
          db.completePlan(args.id)
          log.info(s"Plan #${args.id} completed")
          complete(StatusCodes.OK)
        }else
          complete(StatusCodes.Forbidden)
      }
      }
    }
  }

  def failPlan: Route = {
    (post & auth) { user =>
      entity(as[FailPlanArgs]) { args => {
        if (db.userOwnedPlan(user.id, args.id)) {
          db.failPlan(args.id)
          log.info(s"Plan #${args.id} failed")
          complete(StatusCodes.OK)
        }else
          complete(StatusCodes.Forbidden)
      }
      }
    }
  }

  def edit: Route = {
    (post & auth) { user =>
      entity(as[EditPlanArgs]) { args => {
        throwInvalid(args.description)

        if (db.userOwnedPlan(user.id, args.id) && tags.userOwnedTag(user.id, args.tag) && accounts.userOwnedAccount(user.id, args.account)){
          db.editPlan(args.id, args.delta, args.tag, Instant.ofEpochSecond(args.date), args.account, args.description, args.state)
          complete(StatusCodes.OK)
        }else{
          complete(StatusCodes.Forbidden)
        }
      }
      }
    }
  }

  def getFuture: Route = {
    (get & auth) { user =>
      complete(gson.toJson(db.getUserFuturePlans(user.id)))
    }
  }

  def route: Route = {
    path("add") {
      add
    } ~ path("complete") {
      completePlan
    } ~ path("fail") {
      failPlan
    } ~ path("edit") {
      edit
    } ~ path("all") {
      getAll
    } ~ pathEnd {
      getFuture
    }
  }

  class FailPlanArgs(val id: Int) extends GsonMessage
  class CompletePlanArgs(val id: Int) extends GsonMessage
  class EditPlanArgs(val id: Int, val delta: Double, val tag: Int, val date: Int, val account: Int, val description: String, val state: Int) extends GsonMessage
  class NewPlanArgs(val delta: Double, val tag: Int, val date: Int, val account: Int, val description: String) extends GsonMessage
}