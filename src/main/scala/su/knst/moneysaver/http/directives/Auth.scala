package su.knst.moneysaver
package http.directives

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive, Directive1, Route}
import com.google.inject.Inject
import su.knst.moneysaver.exceptions.UserNotAuthorizedException
import su.knst.moneysaver.objects.User
import su.knst.moneysaver.utils.API

import java.util.UUID
import javax.inject.Singleton

@Singleton
class Auth @Inject()(api: API) extends Directive1[User] {
  override def tapply(f: Tuple1[User] => Route): Route = {
    cxt => {
      val token = cxt.request.uri.query().get("token").getOrElse("")
      if (token.isEmpty)
        throw new UserNotAuthorizedException

      try{
        f(Tuple1(
          api.getUser(UUID.fromString(token))
        ))(cxt)
      }catch {
        case u: UserNotAuthorizedException => throw u
        case e: Exception => throw e
      }
    }
  }
}
