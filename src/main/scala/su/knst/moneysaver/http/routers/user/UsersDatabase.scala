package su.knst.moneysaver.http.routers.user

import com.google.inject.{Inject, Singleton}
import org.mindrot.jbcrypt.BCrypt
import su.knst.moneysaver.exceptions.UserNotAuthorizedException
import su.knst.moneysaver.http.routers.accounts.AccountsDatabase
import su.knst.moneysaver.http.routers.plans.PlansDatabase
import su.knst.moneysaver.http.routers.tags.TagsDatabase
import su.knst.moneysaver.objects.{AuthedUser, User, UserSession}
import su.knst.moneysaver.public_.tables.Users.USERS
import su.knst.moneysaver.public_.tables.UsersSessions.USERS_SESSIONS
import su.knst.moneysaver.utils.Database
import su.knst.moneysaver.utils.config.MainConfig

import java.time.{Instant, LocalDateTime, ZoneOffset}
import java.util
import java.util.{Optional, UUID}

@Singleton
class UsersDatabase @Inject()
(
  database: Database,
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

    AuthedUser(authedUser, token)
  }

  def authUser(token: UUID): AuthedUser = AuthedUser(getUser(token), token)

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
      database.context
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