package su.knst.moneysaver.http.routers.plans

import com.google.inject.{Inject, Singleton}
import su.knst.moneysaver.http.routers.transactions.TransactionsDatabase
import su.knst.moneysaver.objects.Plan
import su.knst.moneysaver.public_.tables.Plans.PLANS
import su.knst.moneysaver.utils.Database

import java.time.{Instant, LocalDateTime, ZoneId, ZoneOffset}
import java.util

@Singleton
class PlansDatabase @Inject()
(
  database: Database,
  transactions: TransactionsDatabase
) {
  def getUserPlans(user: Int, offset: Int = 0, count: Int = 0): util.List[Plan] = {
    val request = database.context.selectFrom(PLANS)
      .where(PLANS.USER.eq(user))
      .orderBy(PLANS.DATE.desc(), PLANS.ID.desc())

    if (count > 0)
      request.limit(offset, count)

    request.fetch().map(r => r.into(classOf[Plan]))
  }

  def userOwnedPlan(user: Int, plan: Int): Boolean = {
    database.context
      .select(PLANS.USER)
      .from(PLANS)
      .where(PLANS.ID.eq(plan))
      .fetchOptional().map[Boolean](_.value1() == user).orElse(false)
  }

  def getUserFuturePlans(user: Int): util.List[Plan] = {
    database.context.selectFrom(PLANS)
      .where(PLANS.USER.eq(user).and(PLANS.STATE.eq(0)))
      .orderBy(PLANS.DATE, PLANS.ID)
      .fetch().map(r => r.into(classOf[Plan]))
  }

  def newPlan(user: Int, delta: Double, tag: Int, date: Instant, account: Int, description: String, state: Int): Int = {
    database.context
      .insertInto(PLANS)
      .set(PLANS.USER, Int.box(user))
      .set(PLANS.DELTA, Double.box(delta))
      .set(PLANS.TAG, Int.box(tag))
      .set(PLANS.DATE, LocalDateTime.ofInstant(date, ZoneId.systemDefault()))
      .set(PLANS.ACCOUNT, Int.box(account))
      .set(PLANS.DESCRIPTION, description.trim)
      .set(PLANS.STATE, Int.box(state))
      .returningResult(PLANS.ID)
      .fetchOne().value1()
  }

  def editPlan(id: Int, delta: Double, tag: Int, date: Instant, account: Int, description: String, state: Int): Unit = {
    database.context
      .update(PLANS)
      .set(PLANS.DELTA, Double.box(delta))
      .set(PLANS.TAG, Int.box(tag))
      .set(PLANS.DATE, LocalDateTime.ofInstant(date, ZoneId.systemDefault()))
      .set(PLANS.ACCOUNT, Int.box(account))
      .set(PLANS.DESCRIPTION, description.trim)
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

    transactions.newTransaction(plan.user, plan.delta, plan.tag, h12now.toInstant(ZoneOffset.ofHours(3)), plan.account, plan.description)

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
}