package su.knst.moneysaver.http.directives

import akka.http.scaladsl.server.{Directive1, Route}
import com.google.inject.{Inject, Singleton}
import su.knst.moneysaver.exceptions.UserNotAuthorizedException
import su.knst.moneysaver.http.routers.admin.AdminDatabase
import su.knst.moneysaver.http.routers.user.UsersDatabase
import su.knst.moneysaver.objects.AuthedUser

import java.util.UUID

@Singleton
class AdminAuth @Inject()(users: UsersDatabase, adminDatabase: AdminDatabase) extends Directive1[AuthedUser] {
  override def tapply(f: Tuple1[AuthedUser] => Route): Route = {
    cxt => {
      val token = cxt.request.uri.query().get("token").getOrElse("")
      if (token.isEmpty)
        throw new UserNotAuthorizedException

      val user: AuthedUser = users.authUser(UUID.fromString(token))

      if (!adminDatabase.isAdmin(user.id))
        throw new UserNotAuthorizedException

      try{
        f(Tuple1(user))(cxt)
      }catch {
        case u: UserNotAuthorizedException => throw u
        case e: Exception => throw e
      }
    }
  }
}

