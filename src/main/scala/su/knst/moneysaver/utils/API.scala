package su.knst.moneysaver
package utils

import akka.actor.ActorSystem
import com.google.inject.{Inject, Singleton}
import com.wanari.webpush.{PushService, Subscription, Utils}
import org.jooq.impl.DSL._
import objects.{RepeatTransaction, Transaction, _}
import org.jooq.Configuration
import org.jooq.impl.DSL
import org.mindrot.jbcrypt.BCrypt
import su.knst.moneysaver.exceptions.UserNotAuthorizedException
import su.knst.moneysaver.public_.tables.Plans._
import su.knst.moneysaver.public_.tables.RepeatTransactions.REPEAT_TRANSACTIONS
import su.knst.moneysaver.public_.tables.Tags._
import su.knst.moneysaver.public_.tables.Transactions._
import su.knst.moneysaver.public_.tables.Users._
import su.knst.moneysaver.public_.tables.Accounts._
import su.knst.moneysaver.public_.tables.ServicesData._
import su.knst.moneysaver.public_.tables.UsersNotifications._
import su.knst.moneysaver.services.ServiceCollector
import su.knst.moneysaver.utils.G.gson
import su.knst.moneysaver.utils.config.MainConfig
import su.knst.moneysaver.utils.time.{TaskScheduleScheme, TaskTimes}

import java.security.interfaces.{ECPrivateKey, ECPublicKey}
import java.time.temporal.ChronoUnit
import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._
import java.{lang, util}
import java.util.{Optional, UUID}
import java.time.{Instant, LocalDate, LocalDateTime, ZoneId, ZoneOffset}
import scala.collection.convert.ImplicitConversions.`map AsJavaMap`
import scala.collection.mutable
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
@Singleton
class API @Inject() (
  database: Database,
  config: MainConfig,
  implicit val system: ActorSystem
) {
  private val domain = config.server.saveStringIfNull("url", "https://ms.knst.su/")

  def registerUser(email: String, password: String): UUID = {
    val token = UUID.randomUUID()

    val newId = database.context
      .insertInto(USERS)
      .set(USERS.EMAIL, email)
      .set(USERS.PASSWORD, BCrypt.hashpw(password, BCrypt.gensalt(12)))
      .set(USERS.TOKEN, token)
      .set(USERS.RECEIPT_TOKEN, "")
      .returningResult(USERS.ID)
      .fetchOne().value1()

    val newAccountId = newAccount(newId, "Карта")
    newTag(newId, "Корректировка", 0, 0)
    val newTagId = newTag(newId, "Еда", -1, 5000)

    newPlan(newId, -700, newTagId,
      LocalDateTime.now().plusDays(2).withHour(12).withMinute(0).withSecond(0)
        .toInstant(ZoneOffset.ofHours(3)),
      newAccountId, "Покупка еды", 0
    )

    token
  }

  def authUser(email: String, password: String): User = {
    val optionalUser: Optional[User] =
      database.context.selectFrom(USERS)
      .where(USERS.EMAIL.eq(email))
      .fetchOptional()
      .map(r => r.into(classOf[User]))

    if (optionalUser.isEmpty || !BCrypt.checkpw(password, optionalUser.get().password))
      throw new UserNotAuthorizedException

    val authedUser = optionalUser.get()

    val token = UUID.randomUUID()

    database.context
      .update(USERS)
      .set(USERS.TOKEN, token)
      .where(USERS.ID.eq(authedUser.id))
      .execute()

    new User(authedUser.id, authedUser.email, "****", token, authedUser.receiptToken)
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
      .update(USERS)
      .set(USERS.TOKEN, UUID.randomUUID())
      .where(USERS.ID.eq(id))
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
      .where(USERS.EMAIL.eq(email))
      .fetchOptional()
      .map(r => r.into(classOf[User]))
      .orElseThrow()
  }

  def getUser(token: UUID) : User = {
    database.context.selectFrom(USERS)
      .where(USERS.TOKEN.eq(token))
      .fetchOptional()
      .map(r => r.into(classOf[User]))
      .orElseThrow(() => new UserNotAuthorizedException)
  }

  def getUserTags(user: Int): util.List[Tag] = {
    database.context.selectFrom(TAGS)
      .where(TAGS.USER.eq(user))
      .orderBy(TAGS.ID)
      .fetch().map(r => r.into(classOf[Tag]))
  }

  def newTag(user: Int, name: String, kind: Int, limit: Double): Int = {
    database.context
      .insertInto(TAGS)
      .set(TAGS.USER, Int.box(user))
      .set(TAGS.NAME, name)
      .set(TAGS.KIND, Int.box(kind))
      .set(TAGS.LIMIT, Double.box(limit))
      .returningResult(TAGS.ID)
      .fetchOne().value1()
  }

  def editTag(id: Int, name: String, kind: Int, limit: Double): Unit ={
    database.context
      .update(TAGS)
      .set(TAGS.NAME, name)
      .set(TAGS.KIND, Int.box(kind))
      .set(TAGS.LIMIT, Double.box(limit))
      .where(TAGS.ID.eq(id))
      .execute()
  }

  def getUserPlans(user: Int, offset: Int = 0, count: Int = 0): util.List[Plan] = {
    val request = database.context.selectFrom(PLANS)
      .where(PLANS.USER.eq(user))
      .orderBy(PLANS.DATE.desc(), PLANS.ID.desc())

    if (count > 0)
      request.limit(offset, count)

    request.fetch().map(r => r.into(classOf[Plan]))
  }

  def getUserTransactions(user: Int, offset: Int, count: Int): util.List[Transaction] = {
    database.context.selectFrom(TRANSACTIONS)
      .where(TRANSACTIONS.USER.eq(user))
      .orderBy(TRANSACTIONS.DATE.desc, TRANSACTIONS.ID.desc)
      .limit(offset, count)
      .fetch().map(r => r.into(classOf[Transaction]))
  }

  def getUserTransactionsPerMonth(user: Int, offset: Int = 0, count: Int = 0): util.List[Transaction] = {
    val time = LocalDateTime.now()

    val request = database.context.selectFrom(TRANSACTIONS)
      .where(
        TRANSACTIONS.USER.eq(user)
        .and(TRANSACTIONS.DATE.greaterThan(LocalDateTime.of(time.getYear, time.getMonth, 1,0,0,0)))
      )
      .orderBy(TRANSACTIONS.DATE.desc, TRANSACTIONS.ID.desc)

    if (count > 0)
      request.limit(offset, count)

    request.fetch().map(r => r.into(classOf[Transaction]))
  }

  def getUserTransactionsPerYear(user: Int, year: Int, offset: Int = 0, count: Int = 0): util.List[Transaction] = {
    val request = database.context.selectFrom(TRANSACTIONS)
      .where(
        TRANSACTIONS.USER.eq(user)
          .and(DSL.year(TRANSACTIONS.DATE).eq(year))
      )
      .orderBy(TRANSACTIONS.DATE.desc, TRANSACTIONS.ID.desc)

    if (count > 0)
      request.limit(offset, count)

    request.fetch().map(r => r.into(classOf[Transaction]))
  }

  def getUserRepeatTransactions(user: Int, offset: Int, count: Int): util.List[RepeatTransaction] = {
    database.context.selectFrom(REPEAT_TRANSACTIONS)
      .where(REPEAT_TRANSACTIONS.USER.eq(user))
      .orderBy(REPEAT_TRANSACTIONS.ID.desc)
      .limit(offset, count)
      .fetch().map(r => r.into(classOf[RepeatTransaction]))
  }

  def userOwnRepeatTransaction(user: Int, transaction: Int) : Boolean = {
    database.context
      .select(REPEAT_TRANSACTIONS.USER)
      .from(REPEAT_TRANSACTIONS)
      .where(REPEAT_TRANSACTIONS.ID.eq(transaction))
      .fetchOptional().map[Boolean](r => r.value1().equals(user)).orElse(false)
  }

  def newRepeatTransaction(user: Int, tag: Int, delta: Double, account: Int, arg: Int, startTime: Instant, repeatFunc: Int, description: String): Unit = {
    val lr: LocalDateTime = LocalDateTime.ofInstant(startTime, ZoneId.systemDefault())

    database.context
      .insertInto(REPEAT_TRANSACTIONS)
      .set(REPEAT_TRANSACTIONS.USER, Int.box(user))
      .set(REPEAT_TRANSACTIONS.TAG, Int.box(tag))
      .set(REPEAT_TRANSACTIONS.DELTA, Double.box(delta))
      .set(REPEAT_TRANSACTIONS.ACCOUNT, Int.box(account))
      .set(REPEAT_TRANSACTIONS.ARG, Int.box(arg))
      .set(REPEAT_TRANSACTIONS.LAST_REPEAT, lr)
      .set(REPEAT_TRANSACTIONS.NEXT_REPEAT, TaskTimes.get(repeatFunc)(lr, arg).withHour(12))
      .set(REPEAT_TRANSACTIONS.REPEAT_FUNC, Int.box(repeatFunc))
      .set(REPEAT_TRANSACTIONS.DESCRIPTION, description)
      .execute()
  }

  def editRepeatTransaction(id: Int, tag: Int, delta: Double, account: Int, arg: Int, lastRepeat: Instant, repeatFunc: Int, description: String): Unit = {
    val lr: LocalDateTime = LocalDateTime.ofInstant(lastRepeat, ZoneId.systemDefault())

    database.context
      .update(REPEAT_TRANSACTIONS)
      .set(REPEAT_TRANSACTIONS.TAG, Int.box(tag))
      .set(REPEAT_TRANSACTIONS.DELTA, Double.box(delta))
      .set(REPEAT_TRANSACTIONS.ACCOUNT, Int.box(account))
      .set(REPEAT_TRANSACTIONS.ARG, Int.box(arg))
      .set(REPEAT_TRANSACTIONS.LAST_REPEAT, lr)
      .set(REPEAT_TRANSACTIONS.NEXT_REPEAT, TaskTimes.get(repeatFunc)(lr, arg).withHour(12))
      .set(REPEAT_TRANSACTIONS.REPEAT_FUNC, Int.box(repeatFunc))
      .set(REPEAT_TRANSACTIONS.DESCRIPTION, description)
      .where(REPEAT_TRANSACTIONS.ID.eq(Int.box(id)))
      .execute()
  }

  def processRepeatTransactions(): Unit = {
    val timeNow: LocalDateTime = LocalDateTime.now()

    def process(id: Int, configuration: Configuration): Unit = {
      val rt = DSL.using(configuration)
        .selectFrom(REPEAT_TRANSACTIONS)
        .where(REPEAT_TRANSACTIONS.ID.eq(id))
        .fetchOptional()
        .map(_.into(classOf[RepeatTransaction]))
        .orElseThrow()

      DSL.using(configuration)
        .insertInto(TRANSACTIONS)
        .set(TRANSACTIONS.USER, Int.box(rt.user))
        .set(TRANSACTIONS.DELTA, Double.box(rt.delta))
        .set(TRANSACTIONS.TAG, Int.box(rt.tag))
        .set(TRANSACTIONS.DATE, LocalDateTime.ofInstant(rt.nextRepeat, ZoneId.systemDefault()))
        .set(TRANSACTIONS.ACCOUNT, Int.box(rt.account))
        .set(TRANSACTIONS.DESCRIPTION, rt.description + " [повтор. транзакция]")
        .execute()

      DSL.using(configuration)
        .update(ACCOUNTS)
        .set(ACCOUNTS.AMOUNT, ACCOUNTS.AMOUNT.plus(rt.delta))
        .where(ACCOUNTS.ID.eq(rt.account))
        .execute()

      var nextRepeat = TaskTimes
        .get(rt.repeatFunc)(LocalDateTime.ofInstant(rt.lastRepeat, ZoneId.systemDefault()), rt.arg)

      if (nextRepeat.isBefore(LocalDateTime.now()))
        nextRepeat = TaskTimes.get(rt.repeatFunc)(timeNow, rt.arg)

      DSL.using(configuration)
        .update(REPEAT_TRANSACTIONS)
        .set(REPEAT_TRANSACTIONS.LAST_REPEAT, LocalDateTime.ofInstant(rt.nextRepeat, ZoneId.systemDefault()))
        .set(REPEAT_TRANSACTIONS.NEXT_REPEAT, nextRepeat)
        .where(REPEAT_TRANSACTIONS.ID.eq(id))
        .execute()

      val tagName = DSL.using(configuration)
        .select(TAGS.NAME)
        .from(TAGS)
        .where(TAGS.ID.eq(rt.tag))
        .fetchOptional().toScala
        .map(_.value1())
        .getOrElse("неизвестно")

      sendNotificationToUser(rt.user,
        new PushNotification("Автоматическая транзакция исполнилась",
          s"Сумма: ${rt.delta}, тег: $tagName",
          s"${domain}icon.png",
          s"${domain}icon.png",
          s"$domain"
        )
      )
    }

    database.context.transaction(configuration => {
      val ids: util.List[Integer] = DSL.using(configuration)
        .select(REPEAT_TRANSACTIONS.ID)
        .from(REPEAT_TRANSACTIONS)
        .where(REPEAT_TRANSACTIONS.NEXT_REPEAT.lessThan(LocalDateTime.now()))
        .forUpdate()
        .fetch().map(_.value1())

      ids.forEach(process(_, configuration))
    })
  }

  def removeRepeatTransaction(id: Int): Unit = {
    database.context
      .delete(REPEAT_TRANSACTIONS)
      .where(REPEAT_TRANSACTIONS.ID.eq(id))
      .execute()
  }

  def newTransaction(user: Int, delta: Double, tag: Int, date: Instant, account: Int, description: String): Unit = {
    database.context
      .insertInto(TRANSACTIONS)
      .set(TRANSACTIONS.USER, Int.box(user))
      .set(TRANSACTIONS.DELTA, Double.box(delta))
      .set(TRANSACTIONS.TAG, Int.box(tag))
      .set(TRANSACTIONS.DATE, LocalDateTime.ofInstant(date, ZoneId.systemDefault()))
      .set(TRANSACTIONS.ACCOUNT, Int.box(account))
      .set(TRANSACTIONS.DESCRIPTION, description)
      .execute()

    database.context
      .update(ACCOUNTS)
      .set(ACCOUNTS.AMOUNT, ACCOUNTS.AMOUNT.plus(delta))
      .where(ACCOUNTS.ID.eq(account))
      .execute()
  }

  def userOwnedTransaction(user: Int, transaction: Int): Boolean = {
    database.context
      .select(TRANSACTIONS.USER)
      .from(TRANSACTIONS)
      .where(TRANSACTIONS.ID.eq(transaction))
      .fetchOptional().map[Boolean](_.value1() == user).orElse(false)
  }

  def userOwnedTag(user: Int, tag: Int): Boolean = {
    database.context
      .select(TAGS.USER)
      .from(TAGS)
      .where(TAGS.ID.eq(tag))
      .fetchOptional().map[Boolean](_.value1() == user).orElse(false)
  }

  def userOwnedAccount(user: Int, account: Int): Boolean = {
    database.context
      .select(ACCOUNTS.USER)
      .from(ACCOUNTS)
      .where(ACCOUNTS.ID.eq(account))
      .fetchOptional().map[Boolean](_.value1() == user).orElse(false)
  }

  def userOwnedPlan(user: Int, plan: Int): Boolean = {
    database.context
      .select(PLANS.USER)
      .from(PLANS)
      .where(PLANS.ID.eq(plan))
      .fetchOptional().map[Boolean](_.value1() == user).orElse(false)
  }

  def cancelTransaction(id: Int): Unit ={
    database.context.transaction(configuration => {
      val optionalTransaction: Optional[Transaction] = DSL.using(configuration)
        .selectFrom(TRANSACTIONS)
        .where(TRANSACTIONS.ID.eq(id))
        .forUpdate()
        .fetchOptional()
        .map(_.into(classOf[Transaction]))

      if (!optionalTransaction.isEmpty){
        val transaction: Transaction = optionalTransaction.get()

        database.context
          .update(ACCOUNTS)
          .set(ACCOUNTS.AMOUNT, ACCOUNTS.AMOUNT.minus(transaction.delta))
          .where(ACCOUNTS.ID.eq(transaction.account))
          .execute()

        database.context
          .delete(TRANSACTIONS)
          .where(TRANSACTIONS.ID.eq(id))
          .execute()
      }

    })
  }

  def getUserFuturePlans(user: Int): util.List[Plan] = {
    database.context.selectFrom(PLANS)
      .where(PLANS.USER.eq(user).and(PLANS.STATE.eq(0)))
      .orderBy(PLANS.DATE, PLANS.ID)
      .fetch().map(r => r.into(classOf[Plan]))
  }

  def newPlan(user: Int, delta: Double, tag: Int, date: Instant, account: Int, description: String, state: Int): Unit = {
    database.context
      .insertInto(PLANS)
      .set(PLANS.USER, Int.box(user))
      .set(PLANS.DELTA, Double.box(delta))
      .set(PLANS.TAG, Int.box(tag))
      .set(PLANS.DATE, LocalDateTime.ofInstant(date, ZoneId.systemDefault()))
      .set(PLANS.ACCOUNT, Int.box(account))
      .set(PLANS.DESCRIPTION, description)
      .set(PLANS.STATE, Int.box(state))
      .execute()
  }

  def editPlan(id: Int, delta: Double, tag: Int, date: Instant, account: Int, description: String, state: Int): Unit = {
    database.context
      .update(PLANS)
      .set(PLANS.DELTA, Double.box(delta))
      .set(PLANS.TAG, Int.box(tag))
      .set(PLANS.DATE, LocalDateTime.ofInstant(date, ZoneId.systemDefault()))
      .set(PLANS.ACCOUNT, Int.box(account))
      .set(PLANS.DESCRIPTION, description)
      .set(PLANS.STATE, Int.box(state))
      .where(PLANS.ID.eq(id))
      .execute()
  }

  def completePlan(id: Int): Unit = {
    val plan : Plan = database.context
      .selectFrom(PLANS)
      .where(PLANS.ID.eq(id))
      .fetchOptional().orElseThrow()
      .into(classOf[Plan])

    val now = LocalDateTime.now()
    val h12now = LocalDateTime.of(now.getYear,now.getMonth,now.getDayOfMonth, 12, 0, 0);

    newTransaction(plan.user, plan.delta, plan.tag, h12now.toInstant(ZoneOffset.ofHours(3)), plan.account, plan.description)

    database.context
      .update(PLANS)
      .set(PLANS.STATE, Int.box(1))
      .where(PLANS.ID.eq(id))
      .execute()
  }

  def failPlan(id: Int): Unit = {
    database.context
      .update(PLANS)
      .set(PLANS.STATE, Int.box(-1))
      .where(PLANS.ID.eq(id))
      .execute()
  }

  def newAccount(user: Int, name: String): Int ={
    database.context
      .insertInto(ACCOUNTS)
      .set(ACCOUNTS.USER, Int.box(user))
      .set(ACCOUNTS.NAME, name)
      .set(ACCOUNTS.AMOUNT, Double.box(0))
      .returningResult(ACCOUNTS.ID)
      .fetchOne().value1()
  }

  def setAccountName(id: Int, name: String): Unit ={
    database.context
      .update(ACCOUNTS)
      .set(ACCOUNTS.NAME, name)
      .where(ACCOUNTS.ID.eq(id))
      .execute()
  }

  def getUserAccounts(user: Int): util.List[Account] = {
    database.context
      .selectFrom(ACCOUNTS)
      .where(ACCOUNTS.USER.eq(user))
      .orderBy(ACCOUNTS.ID)
      .fetch().map(r => r.into(classOf[Account]))
  }

  def changesPerMonthByTags(user: Int): util.Map[Int, Double] ={
    val transactions : mutable.Buffer[Transaction] = getUserTransactionsPerMonth(user).asScala

    var result : Map[Int, Double] =
      transactions
      .groupMap(_.tag)(_.delta)
      .map { case (k, v) => k -> v.sum }

    getUserTags(user).asScala.foreach(t => {
      if (!result.contains(t.id)) result = result + (t.id -> 0)
    })

    result.asJava
  }

  def changesPerYear(user: Int, year: Int): util.Map[Int, util.Map[Int, Double]]  = {
    val transactions: mutable.Buffer[Transaction] = getUserTransactionsPerYear(user, year).asScala

    transactions
      .groupMap(t => LocalDateTime.ofInstant(t.date, ZoneId.systemDefault()).getMonthValue)(identity)
      .map {
        case (d, t) =>
          d -> t.groupMap(_.tag)(_.delta).map { case (k, v) => k -> v.sum }.asJava
      }.asJava
  }

  def getServiceData(service: String, name: String): Option[String] = {
    database.context
      .select(SERVICES_DATA.DATA)
      .from(SERVICES_DATA)
      .where(SERVICES_DATA.SERVICE.eq(service).and(SERVICES_DATA.VAR_NAME.eq(name)))
      .fetchOptional().map[String](_.value1()).toScala
  }

  def setServiceData(service: String, name: String, data: String): Unit = {
    database.context
      .insertInto(SERVICES_DATA)
      .set(SERVICES_DATA.SERVICE, service)
      .set(SERVICES_DATA.VAR_NAME, name)
      .set(SERVICES_DATA.DATA, data)
      .onConflict(SERVICES_DATA.VAR_NAME)
      .doUpdate()
      .set(SERVICES_DATA.DATA, data)
      .where(SERVICES_DATA.VAR_NAME.eq(name).and(SERVICES_DATA.SERVICE.eq(service)))
      .execute()
  }

  def addUserNotificationData(user: Int, endpoint: String, auth: String, p256dh: String): Unit = {
    database.context
      .insertInto(USERS_NOTIFICATIONS)
      .set(USERS_NOTIFICATIONS.USER_ID, Int.box(user))
      .set(USERS_NOTIFICATIONS.ENDPOINT, endpoint)
      .set(USERS_NOTIFICATIONS.AUTH, auth)
      .set(USERS_NOTIFICATIONS.P256DH, p256dh)
      .execute()
  }

  def sendNotificationToUser(user: Int, notification: PushNotification): Unit = {
    val publicKey = config.webPush.saveStringIfNull("publicKey", "XXX")
    val privateKey = config.webPush.saveStringIfNull("privateKey", "XXX")

    if (publicKey.equals("XXX") || privateKey.equals("XXX"))
      return

    val vapidPublicKey: ECPublicKey = Utils.loadPublicKey(publicKey)
    val vapidPrivateKey: ECPrivateKey = Utils.loadPrivateKey(privateKey)

    val pushService = PushService(vapidPublicKey, vapidPrivateKey, domain)
    val jsonNotify = gson.toJson(notification)

    database.context
      .selectFrom(USERS_NOTIFICATIONS)
      .where(USERS_NOTIFICATIONS.USER_ID.eq(user))
      .fetchArray()
      .map[UserNotificationData](_.into(classOf[UserNotificationData]))
      .map(d => Subscription(d.endpoint, d.p256dh, d.auth))
      .foreach(pushService.send(_, jsonNotify))
  }
}