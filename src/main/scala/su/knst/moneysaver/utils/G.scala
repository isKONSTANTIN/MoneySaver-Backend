package su.knst.moneysaver
package utils

import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, PredefinedFromEntityUnmarshallers}

import scala.reflect.ClassTag

object G {
  val gson = new com.google.gson.Gson()

  implicit def gsonUnmarshaller[T <: GsonMessage](implicit ct: ClassTag[T]): FromEntityUnmarshaller[T] =
    PredefinedFromEntityUnmarshallers.stringUnmarshaller.map { str =>
      try {
        gson.fromJson(str, ct.runtimeClass).asInstanceOf[T]
      }catch {
        case e: Exception => {
          e.printStackTrace()
          throw e
        }

      }

    }
}
