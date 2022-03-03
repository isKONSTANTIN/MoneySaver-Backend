package su.knst.moneysaver.http.routers.user

import com.google.inject.{Inject, Singleton}
import org.mindrot.jbcrypt.BCrypt
import su.knst.moneysaver.exceptions.UserNotAuthorizedException
import su.knst.moneysaver.http.routers.accounts.AccountsDatabase
import su.knst.moneysaver.http.routers.admin.AdminDatabase
import su.knst.moneysaver.http.routers.plans.PlansDatabase
import su.knst.moneysaver.http.routers.tags.TagsDatabase
import su.knst.moneysaver.jooq.tables.UserRegistrations.USER_REGISTRATIONS
import su.knst.moneysaver.jooq.tables.Users.USERS
import su.knst.moneysaver.jooq.tables.UsersSessions.USERS_SESSIONS
import su.knst.moneysaver.objects.{AuthedUser, User, UserSession}
import su.knst.moneysaver.utils.Database
import su.knst.moneysaver.utils.config.MainConfig

import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.util
import java.util.{Optional, UUID}

@Singleton
class UsersDatabase @Inject()
(
  database: Database,
  adminDatabase: AdminDatabase,
  accounts: AccountsDatabase,
  tags: TagsDatabase,
  plans: PlansDatabase
) {

  def registerUser(email: String, password: String): UUID = {
    val token = UUID.randomUUID()

    val newId = database.context
      .insertInto(USERS)
      .set(USERS.EMAIL, email.trim)
      .set(USERS.PASSWORD, BCrypt.hashpw(password, BCrypt.gensalt(12)))
      .set(USERS.RECEIPT_TOKEN, "")
      .returningResult(USERS.ID)
      .fetchOne().value1()

    database.context
      .insertInto(USERS_SESSIONS)
      .set(USERS_SESSIONS.USER, newId)
      .set(USERS_SESSIONS.SESSION, token)
      .set(USERS_SESSIONS.EXPIRED_AT, LocalDateTime.now().plusDays(7))
      .execute()

    val newAccountId = accounts.newAccount(newId, "Карта")
    tags.newTag(newId, "Корректировка", 0, 0)
    val newTagId = tags.newTag(newId, "Еда", -1, 5000)

    plans.newPlan(newId, -700, newTagId,
      LocalDateTime.now().plusDays(2).withHour(12).withMinute(0).withSecond(0)
        .toInstant(ZoneOffset.ofHours(3)),
      newAccountId, "Покупка еды", 0
    )

    token
  }

  def userExist(email: String): Boolean = {
    database.context
      .fetchExists(
        database.context
        .selectFrom(USERS)
        .where(USERS.EMAIL.eq(email))
      )
  }

  def authUser(email: String, password: String): AuthedUser = {
    val optionalUser: Optional[User] = database.context
      .selectFrom(USERS)
      .where(USERS.EMAIL.eq(email.trim))
      .fetchOptional()
      .map(r => r.into(classOf[User]))

    if (optionalUser.isEmpty || !BCrypt.checkpw(password, optionalUser.get().password))
      throw new UserNotAuthorizedException

    val authedUser = optionalUser.get()
    val token = UUID.randomUUID()

    database.context
      .insertInto(USERS_SESSIONS)
      .set(USERS_SESSIONS.USER, Int.box(authedUser.id))
      .set(USERS_SESSIONS.SESSION, token)
      .set(USERS_SESSIONS.EXPIRED_AT, LocalDateTime.now().plusDays(7))
      .execute()

    AuthedUser(authedUser, token, adminDatabase.isAdmin(authedUser.id))
  }

  def authUser(token: UUID): AuthedUser = {
    val user = getUser(token)
    AuthedUser(user, token, adminDatabase.isAdmin(user.id))
  }

  def updateUserReceiptToken(id: Int, token: String): Unit = {
    database.context
      .update(USERS)
      .set(USERS.RECEIPT_TOKEN, token)
      .where(USERS.ID.eq(id))
      .execute()
  }

  def changePasswordUser(id: Int, password: String) : Unit = {
    database.context
      .update(USERS)
      .set(USERS.PASSWORD, BCrypt.hashpw(password, BCrypt.gensalt(12)))
      .where(USERS.ID.eq(id))
      .execute()

    database.context
      .update(USERS_SESSIONS)
      .set(USERS_SESSIONS.EXPIRED_AT, LocalDateTime.now().minusSeconds(1))
      .where(USERS_SESSIONS.USER.eq(id))
      .execute()
  }

  def checkPassword(id: Int, password: String) : Boolean = {
    database.context
      .select(USERS.PASSWORD)
      .from(USERS)
      .where(USERS.ID.eq(id))
      .fetchOptional()
      .map(_.value1()).map(BCrypt.checkpw(password, _))
      .orElseThrow()
  }

  def getUser(id: Int) : User = {
    database.context.selectFrom(USERS)
      .where(USERS.ID.eq(id))
      .fetchOptional()
      .map(r => r.into(classOf[User]))
      .orElseThrow()
  }

  def getUser(email: String) : User = {
    database.context
      .selectFrom(USERS)
      .where(USERS.EMAIL.eq(email.trim))
      .fetchOptional()
      .map(r => r.into(classOf[User]))
      .orElseThrow()
  }

  def getUser(token: UUID) : User = {
    val session: UserSession = database.context
      .selectFrom(USERS_SESSIONS)
      .where(USERS_SESSIONS.SESSION.eq(token))
      .fetchOptional()
      .map(r => r.into(classOf[UserSession]))
      .orElseThrow(() => new UserNotAuthorizedException)

    if (session.expiredAt.isAfter(Instant.now())){
      if (session.expiredAt.isBefore(Instant.now().plus(6, ChronoUnit.DAYS)))
        database.context // TODO: check whats wrong with it, take >30 ms
          .update(USERS_SESSIONS)
          .set(USERS_SESSIONS.EXPIRED_AT, LocalDateTime.now().plusDays(7))
          .where(USERS_SESSIONS.ID.eq(session.id))
          .execute()

      getUser(session.user)
    }else{
      throw new UserNotAuthorizedException
    }
  }

  def getUserSessions(user: Int): util.List[UserSession] = {
    database.context
      .selectFrom(USERS_SESSIONS)
      .where(USERS_SESSIONS.USER.eq(user))
      .fetch()
      .map(_.into(classOf[UserSession]))
  }

  def getUserActiveSessions(user: Int): util.List[UserSession] = {
    database.context
      .selectFrom(USERS_SESSIONS)
      .where(
        USERS_SESSIONS.USER.eq(user)
          .and(USERS_SESSIONS.EXPIRED_AT.greaterThan(LocalDateTime.now()))
      )
      .fetch()
      .map(_.into(classOf[UserSession]))
  }

  def deactivateUserSession(token: UUID): Unit = {
    database.context
      .update(USERS_SESSIONS)
      .set(USERS_SESSIONS.EXPIRED_AT, LocalDateTime.now().minusSeconds(1))
      .where(USERS_SESSIONS.SESSION.eq(token))
      .execute()
  }
}
