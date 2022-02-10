package su.knst.moneysaver.services

import com.google.inject.{Inject, Singleton}
import su.knst.moneysaver.public_.tables.ServicesData.SERVICES_DATA
import su.knst.moneysaver.utils.Database

import scala.jdk.OptionConverters.RichOptional

@Singleton
class ServicesDatabase @Inject()
(
  database: Database
) {
  def getServiceData(service: String, name: String): Option[String] = {
    database.context
      .select(SERVICES_DATA.DATA)
      .from(SERVICES_DATA)
      .where(SERVICES_DATA.SERVICE.eq(service).and(SERVICES_DATA.VAR_NAME.eq(name)))
      .fetchOptional().map[String](_.value1()).toScala
  }

  def setServiceData(service: String, name: String, data: String): Unit = {
    database.context
      .insertInto(SERVICES_DATA)
      .set(SERVICES_DATA.SERVICE, service)
      .set(SERVICES_DATA.VAR_NAME, name)
      .set(SERVICES_DATA.DATA, data)
      .onConflict(SERVICES_DATA.VAR_NAME)
      .doUpdate()
      .set(SERVICES_DATA.DATA, data)
      .where(SERVICES_DATA.VAR_NAME.eq(name).and(SERVICES_DATA.SERVICE.eq(service)))
      .execute()
  }
}
