package su.knst.moneysaver.http.routers.server

import scala.jdk.CollectionConverters._
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives.{as, complete, entity, get, path, pathEnd, post}
import akka.http.scaladsl.server.Route
import com.google.inject.Inject
import su.knst.moneysaver.http.routers.accounts.AccountsDatabase
import su.knst.moneysaver.http.routers.tags.TagsDatabase
import su.knst.moneysaver.objects.{Plan, Tag}
import su.knst.moneysaver.utils.logger.DefaultLogger

import java.time.Instant
import scala.collection.mutable
import akka.http.scaladsl.server.Directives.{path, pathEnd}
import akka.http.scaladsl.server.Route
import com.google.inject.Inject
import su.knst.moneysaver.http.directives.Auth
import su.knst.moneysaver.http.routers.accounts.AccountsDatabase
import su.knst.moneysaver.utils.G.gson
import su.knst.moneysaver.{http, utils}
import su.knst.moneysaver.utils.{HttpResult, ServerOptions}
import su.knst.moneysaver.utils.logger.DefaultLogger

class ServerInfoRouter @Inject()
(
  options: ServerOptions
) {
  protected val log: DefaultLogger = DefaultLogger("http", "server_info")

  def info: Route = {
    get {
      complete(gson.toJson(new ServerInfo(options.version, options.registration)))
    }
  }

  def route: Route = {
    pathEnd {
      info
    }
  }

  class ServerInfo(val version: String, val registration: Boolean) extends HttpResult
}
