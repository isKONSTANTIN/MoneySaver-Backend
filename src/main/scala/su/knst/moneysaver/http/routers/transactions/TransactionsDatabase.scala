package su.knst.moneysaver.http.routers.transactions

import com.google.inject.{Inject, Singleton}
import org.jooq.impl.DSL
import su.knst.moneysaver.objects.{RepeatTransaction, Transaction}
import su.knst.moneysaver.jooq.tables.Accounts.ACCOUNTS
import su.knst.moneysaver.jooq.tables.RepeatTransactions.REPEAT_TRANSACTIONS
import su.knst.moneysaver.jooq.tables.Transactions.TRANSACTIONS
import su.knst.moneysaver.utils.Database
import su.knst.moneysaver.utils.time.TaskTimes

import java.time.{Instant, LocalDate, LocalDateTime, LocalTime, ZoneId}
import java.util
import java.util.Optional

@Singleton
class TransactionsDatabase @Inject()
(
  database: Database
) {
  def editTransaction(id: Int, delta: Double, tag: Int, date: Instant, account: Int, description: String): Unit ={
    database.context.transaction(configuration => {
      val oldTransaction = DSL.using(configuration)
        .selectFrom(TRANSACTIONS)
        .where(TRANSACTIONS.ID.eq(id))
        .forUpdate()
        .fetchOptional()
        .map(_.into(classOf[Transaction]))
        .orElseThrow()

      DSL.using(configuration)
        .update(TRANSACTIONS)
        .set(TRANSACTIONS.DELTA, Double.box(delta))
        .set(TRANSACTIONS.TAG, Int.box(tag))
        .set(TRANSACTIONS.DATE, LocalDateTime.ofInstant(date, ZoneId.systemDefault()))
        .set(TRANSACTIONS.ACCOUNT, Int.box(account))
        .set(TRANSACTIONS.DESCRIPTION, description)
        .where(TRANSACTIONS.ID.eq(id))
        .execute()

      DSL.using(configuration)
        .update(ACCOUNTS)
        .set(ACCOUNTS.AMOUNT, ACCOUNTS.AMOUNT.minus(oldTransaction.delta))
        .where(ACCOUNTS.ID.eq(oldTransaction.account))
        .execute()

      DSL.using(configuration)
        .update(ACCOUNTS)
        .set(ACCOUNTS.AMOUNT, ACCOUNTS.AMOUNT.plus(delta))
        .where(ACCOUNTS.ID.eq(account))
        .execute()

    })
  }

  def userOwnedTransaction(user: Int, transaction: Int): Boolean = {
    database.context
      .select(TRANSACTIONS.USER)
      .from(TRANSACTIONS)
      .where(TRANSACTIONS.ID.eq(transaction))
      .fetchOptional().map[Boolean](_.value1() == user).orElse(false)
  }

  def cancelTransaction(id: Int): Unit ={
    database.context.transaction(configuration => {
      val optionalTransaction: Optional[Transaction] =
        DSL.using(configuration)
          .selectFrom(TRANSACTIONS)
          .where(TRANSACTIONS.ID.eq(id))
          .forUpdate()
          .fetchOptional()
          .map(_.into(classOf[Transaction]))

      if (!optionalTransaction.isEmpty){
        val transaction: Transaction = optionalTransaction.get()

        DSL.using(configuration)
          .update(ACCOUNTS)
          .set(ACCOUNTS.AMOUNT, ACCOUNTS.AMOUNT.minus(transaction.delta))
          .where(ACCOUNTS.ID.eq(transaction.account))
          .execute()

        DSL.using(configuration)
          .delete(TRANSACTIONS)
          .where(TRANSACTIONS.ID.eq(id))
          .execute()
      }

    })
  }

  def transactionsAtDay(user: Int, date: LocalDate): util.List[Transaction] = {
    database.context
      .selectFrom(TRANSACTIONS)
      .where(TRANSACTIONS.USER.eq(user)
        .and(TRANSACTIONS.DATE.between(date.atStartOfDay(), date.atTime(LocalTime.MAX)))
      )
      .fetch()
      .map(r => r.into(classOf[Transaction]))
  }

  def newTransaction(user: Int, delta: Double, tag: Int, date: Instant, account: Int, description: String): Int = {
    val newId = database.context
      .insertInto(TRANSACTIONS)
      .set(TRANSACTIONS.USER, Int.box(user))
      .set(TRANSACTIONS.DELTA, Double.box(delta))
      .set(TRANSACTIONS.TAG, Int.box(tag))
      .set(TRANSACTIONS.DATE, LocalDateTime.ofInstant(date, ZoneId.systemDefault()))
      .set(TRANSACTIONS.ACCOUNT, Int.box(account))
      .set(TRANSACTIONS.DESCRIPTION, description.trim)
      .returningResult(TRANSACTIONS.ID)
      .fetchOne().value1()

    database.context
      .update(ACCOUNTS)
      .set(ACCOUNTS.AMOUNT, ACCOUNTS.AMOUNT.plus(delta))
      .where(ACCOUNTS.ID.eq(account))
      .execute()

    newId
  }

  def getUserTransactions(user: Int, offset: Int, count: Int, filter: TransactionsFilter = TransactionsFilter.EMPTY): util.List[Transaction] = {
    var request = database.context.selectFrom(TRANSACTIONS)
      .where(TRANSACTIONS.USER.eq(user))

    if (filter.tag != 0)
      request = request.and(TRANSACTIONS.TAG.eq(filter.tag))

    if (filter.account != 0)
      request = request.and(TRANSACTIONS.ACCOUNT.eq(filter.account))

    if (filter.dateFrom != 0)
      request = request.and(TRANSACTIONS.DATE.greaterOrEqual(Instant.ofEpochMilli(filter.dateFrom).atZone(ZoneId.systemDefault()).toLocalDate.atStartOfDay()))

    if (filter.dateUpTo != 0)
      request = request.and(TRANSACTIONS.DATE.lessOrEqual(Instant.ofEpochMilli(filter.dateUpTo).atZone(ZoneId.systemDefault()).toLocalDate.atTime(23, 59)))

    if (filter.descriptionContains != null && filter.descriptionContains.nonEmpty)
      request = request.and(TRANSACTIONS.DESCRIPTION.contains(filter.descriptionContains))

    request
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

  def newRepeatTransaction(user: Int, tag: Int, delta: Double, account: Int, arg: Int, startTime: Instant, repeatFunc: Int, description: String): Int = {
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
      .set(REPEAT_TRANSACTIONS.DESCRIPTION, description.trim)
      .returningResult(REPEAT_TRANSACTIONS.ID)
      .fetchOne().value1()
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
      .set(REPEAT_TRANSACTIONS.DESCRIPTION, description.trim)
      .where(REPEAT_TRANSACTIONS.ID.eq(Int.box(id)))
      .execute()
  }

  def removeRepeatTransaction(id: Int): Unit = {
    database.context
      .delete(REPEAT_TRANSACTIONS)
      .where(REPEAT_TRANSACTIONS.ID.eq(id))
      .execute()
  }
}
