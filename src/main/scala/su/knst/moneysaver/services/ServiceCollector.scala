package su.knst.moneysaver.services

import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}
import javax.inject.{Inject, Singleton}

@Singleton
class ServiceCollector @Inject()
(
  repeatTransactionsService: RepeatTransactionsService
) {
  protected var scheduledExecutorService: ScheduledExecutorService = Executors.newScheduledThreadPool(4)

  scheduledExecutorService.scheduleAtFixedRate(() => {
    repeatTransactionsService.run()
  }, 0, repeatTransactionsService.repeatTime(), TimeUnit.MILLISECONDS)
}
