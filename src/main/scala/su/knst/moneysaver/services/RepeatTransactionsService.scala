package su.knst.moneysaver.services

import com.google.inject.{Inject, Provider, Singleton}
import su.knst.moneysaver.utils.API
import su.knst.moneysaver.utils.logger.DefaultLogger

@Singleton
class RepeatTransactionsService @Inject()
(
  api: API
) extends AbstractService(api) {
  protected val log: DefaultLogger = DefaultLogger("services", "repeat_transactions")

  override def repeatTime(): Long = 5_000

  override def name(): String = "RepeatTransactions"

  override def run(): Unit = {
    try {
      api.processRepeatTransactions()
    }catch {
      case e: Exception =>
        log.error("Error processing repeat transactions:")
        e.printStackTrace()
    }

  }
}
