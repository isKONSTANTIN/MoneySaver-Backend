package su.knst.moneysaver.http.routers.info

import com.google.inject.{Inject, Singleton}
import su.knst.moneysaver.http.routers.tags.TagsDatabase
import su.knst.moneysaver.http.routers.transactions.TransactionsDatabase
import su.knst.moneysaver.objects.Transaction
import su.knst.moneysaver.utils.Database

import java.time.{LocalDate, LocalDateTime, ZoneId}
import java.util
import scala.collection.mutable
import scala.jdk.CollectionConverters.{CollectionHasAsScala, MapHasAsJava}

@Singleton
class UserMainInfoDatabase @Inject()
(
  transactionsDatabase: TransactionsDatabase,
  tags: TagsDatabase
) {
  def changesAtDayByTags(user: Int, date: LocalDate): util.Map[Int, Double] = {
    val transactions = transactionsDatabase.transactionsAtDay(user, date).asScala

    var result : Map[Int, Double] =
      transactions
        .groupMap(_.tag)(_.delta)
        .map { case (k, v) => k -> v.sum }

    tags.getUserTags(user).asScala
      .foreach(t => {
        if (!result.contains(t.id)) result = result + (t.id -> 0)
      })

    result.asJava
  }

  def changesPerMonthByTags(user: Int): util.Map[Int, Double] ={
    val transactions = transactionsDatabase.getUserTransactionsPerMonth(user).asScala

    var result : Map[Int, Double] =
      transactions
        .groupMap(_.tag)(_.delta)
        .map { case (k, v) => k -> v.sum }

    tags.getUserTags(user).asScala.foreach(t => {
      if (!result.contains(t.id)) result = result + (t.id -> 0)
    })

    result.asJava
  }

  def changesPerYear(user: Int, year: Int): util.Map[Int, util.Map[Int, Double]]  = {
    val transactions = transactionsDatabase.getUserTransactionsPerYear(user, year).asScala

    transactions
      .groupMap(t => LocalDateTime.ofInstant(t.date, ZoneId.systemDefault()).getMonthValue)(identity)
      .map {
        case (d, t) =>
          d -> t.groupMap(_.tag)(_.delta).map { case (k, v) => k -> v.sum }.asJava
      }.asJava
  }
}