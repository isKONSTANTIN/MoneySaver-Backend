package su.knst.moneysaver.services

import su.knst.moneysaver.utils.logger.DefaultLogger

import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}
import javax.inject.{Inject, Singleton}

@Singleton
class ServiceCollector @Inject()
(
  repeatTransactionsService: RepeatTransactionsService
) {
  protected val log: DefaultLogger = DefaultLogger("services")

  protected var scheduledExecutorService: ScheduledExecutorService = Executors.newScheduledThreadPool(4)
  protected var services: Int = 0

  startService(repeatTransactionsService)

  def startService(service: AbstractService): Unit = {
    scheduledExecutorService.scheduleAtFixedRate(service, 0, service.repeatTime(), TimeUnit.MILLISECONDS)
    services += 1
    log.info(service.name() + " service started")
  }

  def getServicesCount: Int = services
}
