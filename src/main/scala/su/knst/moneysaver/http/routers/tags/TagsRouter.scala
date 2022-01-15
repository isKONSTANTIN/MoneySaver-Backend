package su.knst.moneysaver
package http.routers.tags

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
import su.knst.moneysaver.objects.Tag

import java.time.Instant
import java.util
import scala.collection.mutable

class TagsRouter @Inject()
(
  api: API,
  auth: Auth
) {

  def add: Route = {
    (post & auth & entity(as[NewTagArgs])) { (user, args) =>
      if (api.getUserTags(user.id).asScala.exists(t => t.name.equals(args.name)))
        complete(StatusCodes.Accepted)
      else {
        api.newTag(user.id, args.name, args.kind, args.limit)

        complete(StatusCodes.Created)
      }

    }
  }

  def edit: Route = {
    (post & auth & entity(as[EditTagArgs])) { (user, args) =>
      if (api.userOwnedTag(user.id, args.id)){
        api.editTag(args.id, args.name, args.kind, args.limit)
        complete(StatusCodes.OK)
      } else
        complete(StatusCodes.BadRequest)
    }
  }

  def main: Route = {
    (get & auth) { user =>
      complete(gson.toJson(api.getUserTags(user.id)))
    }
  }

  def route: Route = {
    path("add") {
      add
    } ~ path("edit") {
      edit
    } ~ pathEnd {
      main
    }
  }

  class NewTagArgs(val name: String, val kind: Int, val limit: Double) extends GsonMessage
  class EditTagArgs(val id: Int, val name: String, val kind: Int, val limit: Double) extends GsonMessage
}
