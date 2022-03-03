package su.knst.moneysaver.utils

import com.google.common.io.Resources
import com.google.inject.{Inject, Singleton}
import org.apache.commons.io.IOUtils
import su.knst.moneysaver.utils.config.MainConfig
import su.knst.moneysaver.utils.logger.DefaultLogger

@Singleton
class ServerOptions @Inject()(config: MainConfig) {
  protected val log: DefaultLogger = DefaultLogger("server")

  val registration: Boolean = config.registration.saveStringIfNull("enabled", "false").toBoolean
  val version: String = IOUtils.toString(classOf[ServerOptions].getResource("/ms_version"))

  log.info(s"Version: $version")
  log.info(s"User registration: $registration")
}
