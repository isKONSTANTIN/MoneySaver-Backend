package su.knst.moneysaver.http.routers.accounts

import com.google.inject.{Inject, Singleton}
import org.jooq.impl.DSL
import su.knst.moneysaver.objects.Account
import su.knst.moneysaver.public_.tables.Accounts.ACCOUNTS
import su.knst.moneysaver.utils.Database

import java.util

@Singleton
class AccountsDatabase @Inject()
(
  database: Database
) {
  def userOwnedAccount(user: Int, account: Int): Boolean = {
    database.context
      .select(ACCOUNTS.USER)
      .from(ACCOUNTS)
      .where(ACCOUNTS.ID.eq(account))
      .fetchOptional().map[Boolean](_.value1() == user).orElse(false)
  }

  def newAccount(user: Int, name: String): Int ={
    database.context
      .insertInto(ACCOUNTS)
      .set(ACCOUNTS.USER, Int.box(user))
      .set(ACCOUNTS.NAME, name.trim)
      .set(ACCOUNTS.AMOUNT, Double.box(0))
      .returningResult(ACCOUNTS.ID)
      .fetchOne().value1()
  }

  def setAccountName(id: Int, name: String): Unit ={
    database.context
      .update(ACCOUNTS)
      .set(ACCOUNTS.NAME, name.trim)
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

  def accountTransfer(from: Int, to: Int, amount: Double): Unit = {
    database.context.transaction(configuration => {
      DSL.using(configuration)
        .update(ACCOUNTS)
        .set(ACCOUNTS.AMOUNT, ACCOUNTS.AMOUNT.minus(amount))
        .where(ACCOUNTS.ID.eq(from))
        .execute()

      DSL.using(configuration)
        .update(ACCOUNTS)
        .set(ACCOUNTS.AMOUNT, ACCOUNTS.AMOUNT.plus(amount))
        .where(ACCOUNTS.ID.eq(to))
        .execute()
    })
  }
}