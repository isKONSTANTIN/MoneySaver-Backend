package su.knst.moneysaver
package http.directives

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive, Directive1, Route}
import com.github.benmanes.caffeine.cache.{AsyncLoadingCache, Cache, Caffeine, LoadingCache}
import com.google.inject.{Inject, Singleton}
import su.knst.moneysaver.exceptions.{UserNotAuthorizedException, UserRegistrationExpired}
import su.knst.moneysaver.http.routers.user.UsersDatabase
import su.knst.moneysaver.http.routers.user.registration.UserRegistrationDatabase
import su.knst.moneysaver.objects.{AuthedUser, User}

import java.util.UUID
import java.util.concurrent.{CompletableFuture, CompletionException, ExecutionException, TimeUnit}
import scala.jdk.CollectionConverters._

@Singleton
class Auth @Inject()(users: UsersDatabase, registration: UserRegistrationDatabase) extends Directive1[AuthedUser] {
  protected val authCache: LoadingCache[UUID, AuthedUser] =
    Caffeine.newBuilder()
      .maximumSize(512)
      .expireAfterWrite(5, TimeUnit.MINUTES)
      .build(token => {
        val user = users.authUser(token)

        if (!registration.userHaveAccess(user.id))
          throw new UserRegistrationExpired

        user
      })

  def invalidateUser(user: Int): Unit = {
    authCache.invalidateAll(
      authCache.asMap().asScala
        .filter(_._2.id == user).keys.asJava
    )
  }

  override def tapply(f: Tuple1[AuthedUser] => Route): Route = {
    cxt => {
      val token = cxt.request.uri.query().get("token").getOrElse("")

      if (token.isEmpty)
        throw new UserNotAuthorizedException

      var uuid: UUID = null

      try {
        uuid = UUID.fromString(token)
      }catch {
        case _: IllegalArgumentException => throw new UserNotAuthorizedException
      }

      try{
        f(Tuple1(authCache.get(uuid)))(cxt)
      }catch {
        case u: UserNotAuthorizedException => throw u
        case e: CompletionException => throw e.getCause
        case e: Exception => throw e
      }
    }
  }
}
