package su.knst.moneysaver.services

import com.google.inject.{Inject, Provider, Singleton}
import org.jooq.Configuration
import org.jooq.impl.DSL
import su.knst.moneysaver.http.routers.pushing.WebPushingDatabase
import su.knst.moneysaver.objects.{PushNotification, RepeatTransaction}
import su.knst.moneysaver.jooq.tables.Accounts.ACCOUNTS
import su.knst.moneysaver.jooq.tables.RepeatTransactions.REPEAT_TRANSACTIONS
import su.knst.moneysaver.jooq.tables.Tags.TAGS
import su.knst.moneysaver.jooq.tables.Transactions.TRANSACTIONS
import su.knst.moneysaver.utils.config.MainConfig
import su.knst.moneysaver.utils.Database
import su.knst.moneysaver.utils.logger.DefaultLogger
import su.knst.moneysaver.utils.time.TaskTimes

import java.time.{LocalDateTime, ZoneId}
import java.util
import scala.jdk.OptionConverters.RichOptional

@Singleton
class RepeatTransactionsService @Inject()
(
  webPushingDatabase: WebPushingDatabase,
  servicesDatabase: ServicesDatabase,
  config: MainConfig,
  database: Database
) extends AbstractService(servicesDatabase) {
  protected val log: DefaultLogger = DefaultLogger("services", "repeat_transactions")
  private implicit val domain: String = config.server.saveStringIfNull("url", System.getenv("BASE_URL"))

  override def repeatTime(): Long = 5_000

  override def name(): String = "RepeatTransactions"

  override def run(): Unit = {
    try {
      processRepeatTransactions()
    }catch {
      case e: Exception =>
        log.error("Error processing repeat transactions:")
        e.printStackTrace()
    }
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

      webPushingDatabase.sendNotificationToUser(rt.user,
        PushNotification.createDefault(
          "Автоматическая транзакция исполнилась",
          s"Сумма: ${rt.delta}, тег: $tagName"
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
}
