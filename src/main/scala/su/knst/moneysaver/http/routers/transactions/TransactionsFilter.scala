package su.knst.moneysaver.http.routers.transactions

import su.knst.moneysaver.utils.GsonMessage

import java.time.Instant

class TransactionsFilter(val tag: Int = 0, val account: Int = 0, val descriptionContains: String = "", val dateFrom: Long = 0, val dateUpTo: Long = 0) extends GsonMessage{

}

object TransactionsFilter {
  val EMPTY = new TransactionsFilter()
}