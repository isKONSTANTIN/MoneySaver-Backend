package su.knst.moneysaver.services

import com.google.inject.{Inject, Provider, Singleton}
import su.knst.moneysaver.utils.API

@Singleton
class RepeatTransactionsService @Inject()
(
  api: API
) extends AbstractService(api) {
  override def repeatTime(): Long = 5_000

  override def name(): String = "RepeatTransactions"

  override def run(): Unit = {
    api.processRepeatTransactions()
  }
}
