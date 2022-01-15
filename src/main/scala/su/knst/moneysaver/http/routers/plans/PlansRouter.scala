package su.knst.moneysaver
package http.routers.plans

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
import su.knst.moneysaver.objects.{Plan, Tag}

import java.time.Instant
import scala.collection.mutable

class PlansRouter @Inject()
(
  api: API,
  auth: Auth
) {

  def add: Route = {
    (post & auth) { user =>
      entity(as[NewPlanArgs]) { args => {
        if (api.userOwnedTag(user.id, args.tag) && api.userOwnedAccount(user.id, args.account)) {
          api.newPlan(user.id, args.delta, args.tag, Instant.ofEpochSecond(args.date), args.account, args.description, 0)
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
      complete(gson.toJson(api.getUserPlans(user.id, offset.getOrElse(0), count.getOrElse(0))))
    }
  }

  def completePlan: Route = {
    (post & auth) { user =>
      entity(as[CompletePlanArgs]) { args => {
        if (api.userOwnedPlan(user.id, args.id)) {
          api.completePlan(args.id)
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
        if (api.userOwnedPlan(user.id, args.id)) {
          api.failPlan(args.id)
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
        if (api.userOwnedPlan(user.id, args.id) && api.userOwnedTag(user.id, args.tag) && api.userOwnedAccount(user.id, args.account)){
          api.editPlan(args.id, args.delta, args.tag, Instant.ofEpochSecond(args.date), args.account, args.description, args.state)
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
      complete(gson.toJson(api.getUserFuturePlans(user.id)))
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