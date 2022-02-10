package su.knst.moneysaver
package http.directives

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive, Directive1, Route}
import com.google.inject.{Inject, Singleton}
import su.knst.moneysaver.exceptions.UserNotAuthorizedException
import su.knst.moneysaver.http.routers.user.UsersDatabase
import su.knst.moneysaver.objects.{AuthedUser, User}

import java.util.UUID

@Singleton
class Auth @Inject()(users: UsersDatabase) extends Directive1[AuthedUser] {
  override def tapply(f: Tuple1[AuthedUser] => Route): Route = {
    cxt => {
      val token = cxt.request.uri.query().get("token").getOrElse("")
      if (token.isEmpty)
        throw new UserNotAuthorizedException

      try{
        f(Tuple1(
          users.authUser(UUID.fromString(token))
        ))(cxt)
      }catch {
        case u: UserNotAuthorizedException => throw u
        case e: Exception => throw e
      }
    }
  }
}
