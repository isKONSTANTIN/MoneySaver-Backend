package su.knst.moneysaver.services

import com.google.inject.Provider

import java.util.Optional
import javax.inject.Inject

abstract class AbstractService(val db: ServicesDatabase) extends Runnable {
  def repeatTime(): Long
  def name(): String

  def getData(name: String): Option[String] = db.getServiceData(this.name(), name)
  def setData(name: String, data: String): Unit = db.setServiceData(this.name(), name, data)

  def getOrSetData(name: String, default: String): String =
    getData(name).getOrElse{ setData(name, default); default }
}
