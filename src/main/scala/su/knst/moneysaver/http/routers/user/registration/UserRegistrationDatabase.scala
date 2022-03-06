package su.knst.moneysaver.http.routers.user.registration

import com.github.benmanes.caffeine.cache.{AsyncLoadingCache, Caffeine, LoadingCache}
import com.google.inject.{Inject, Singleton}
import org.jooq.impl.DSL
import su.knst.moneysaver.exceptions.UserRegistrationExpired
import su.knst.moneysaver.http.routers.admin.AdminDatabase
import su.knst.moneysaver.jooq.tables.UserRegistrations
import su.knst.moneysaver.jooq.tables.UserRegistrations.USER_REGISTRATIONS
import su.knst.moneysaver.objects.{AuthedUser, Transaction, UserRegistrationData}
import su.knst.moneysaver.utils.config.MainConfig
import su.knst.moneysaver.utils.{Database, ServerOptions}

import java.time.LocalDateTime
import java.util.{Optional, UUID}
import java.util.concurrent.TimeUnit

@Singleton
class UserRegistrationDatabase @Inject()
(
  database: Database,
  options: ServerOptions,
  admin: AdminDatabase,
  config: MainConfig
) {
  protected val limitsEnabled: Boolean = options.registration
  protected val userDefaultExpiresDays: Int = config.registration.saveStringIfNull("userDefaultExpiresDays", "14").toInt
  protected val userRegistrationStatusCache: LoadingCache[Integer, java.lang.Boolean] =
    Caffeine.newBuilder()
      .maximumSize(512)
      .expireAfterWrite(5, TimeUnit.MINUTES)
      .build(user => {
        Boolean.box(
          database.context
            .fetchExists(database.context
              .selectFrom(USER_REGISTRATIONS)
              .where(USER_REGISTRATIONS.USER.eq(user))
            ))
      })

  def checkUserRegistration(user: Int): Unit = {
    if (!limitsEnabled || userRegistrationStatusCache.get(user))
      return

    registerUserLimits(user)
  }

  def registerUserLimits(user: Int) : Unit = {
    if (!limitsEnabled)
      return

    val isAdmin = admin.isAdmin(user)

    database.context
      .insertInto(USER_REGISTRATIONS)
      .set(USER_REGISTRATIONS.USER, Int.box(user))
      .set(USER_REGISTRATIONS.REGISTRATION_TIME, LocalDateTime.now())
      .set(USER_REGISTRATIONS.EXPIRES_IN, {
        if (isAdmin) LocalDateTime.MAX else LocalDateTime.now().plusDays(userDefaultExpiresDays)
      })
      .set(USER_REGISTRATIONS.DEMO_ACCOUNT, Boolean.box(!isAdmin))
      .execute()

    userRegistrationStatusCache.invalidate(user)
  }

  def extendUserExpires(user: Int, days: Int): Unit = {
    if (!limitsEnabled)
      return

    checkUserRegistration(user)
    val now = LocalDateTime.now()

    database.context.transaction(configuration => {
      val currentExpires = DSL.using(configuration)
        .select(USER_REGISTRATIONS.EXPIRES_IN)
        .from(USER_REGISTRATIONS)
        .where(USER_REGISTRATIONS.USER.eq(user))
        .fetchOptional().map(_.value1()).orElseThrow()

      val newExpires = {
        if (currentExpires.isBefore(now))
          now.plusDays(days)
        else
          currentExpires.plusDays(days)
      }

      DSL.using(configuration)
        .update(USER_REGISTRATIONS)
        .set(USER_REGISTRATIONS.EXPIRES_IN, newExpires)
        .where(USER_REGISTRATIONS.USER.eq(user))
        .execute()
    })
  }

  def setDemoStatusUser(user: Int, status: Boolean): Unit = {
    if (!limitsEnabled)
      return

    checkUserRegistration(user)

    database.context
      .update(USER_REGISTRATIONS)
      .set(USER_REGISTRATIONS.DEMO_ACCOUNT, Boolean.box(status))
      .where(USER_REGISTRATIONS.USER.eq(user))
      .execute()
  }

  def userHaveAccess(user: Int): Boolean = {
    if (!limitsEnabled)
      return true

    checkUserRegistration(user)

    database.context
      .select(USER_REGISTRATIONS.EXPIRES_IN)
      .from(USER_REGISTRATIONS)
      .where(USER_REGISTRATIONS.USER.eq(user))
      .fetchOptional()
      .map(_.value1().isAfter(LocalDateTime.now()))
      .orElseThrow()
  }

  def getUserRegistrationData(user: Int): UserRegistrationData = {
    if (!limitsEnabled)
      return UserRegistrationData.noLimit(user)

    checkUserRegistration(user)

    database.context
      .selectFrom(USER_REGISTRATIONS)
      .where(USER_REGISTRATIONS.USER.eq(user))
      .fetchOptional()
      .map(_.into(classOf[UserRegistrationData]))
      .orElseThrow()
  }
}