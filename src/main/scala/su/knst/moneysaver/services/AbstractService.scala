package su.knst.moneysaver.services

import com.google.inject.Provider
import su.knst.moneysaver.utils.API

import java.util.Optional
import javax.inject.Inject

abstract class AbstractService(val api: API) extends Runnable {
  def repeatTime(): Long
  def name(): String

  def getData(name: String): Option[String] = api.getServiceData(this.name(), name)
  def setData(name: String, data: String): Unit = api.setServiceData(this.name(), name, data)

  def getOrSetData(name: String, default: String): String =
    getData(name).getOrElse{ setData(name, default); default }
}
