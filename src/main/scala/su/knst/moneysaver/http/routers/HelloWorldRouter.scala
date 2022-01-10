package su.knst.moneysaver
package http.routers

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.PathMatchers.IntNumber
import akka.http.scaladsl.server.{Directive1, Route}
import com.google.inject.Inject
import su.knst.moneysaver.http.directives.Auth
import su.knst.moneysaver.utils.{API, Database}

import java.util.UUID

class HelloWorldRouter @Inject()
(
  api: API,
  auth: Auth
) {
  def route : Route = {
    path(IntNumber) { id =>
      (get & auth) { user =>
        complete("Hello! " + user.email)
      }
    }
  }
}
