package su.knst.moneysaver
package http.routers.info

import akka.http.javadsl.model.HttpCharsets
import akka.http.scaladsl.model.{ContentType, HttpEntity, MediaTypes, StatusCodes}
import http.directives.Auth
import utils.API
import utils.G.gson
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._

import scala.jdk.CollectionConverters._
import com.github.tototoshi.csv.CSVWriter
import com.google.inject.Inject
import su.knst.moneysaver.objects.Tag

import java.io.File
import java.text.DateFormatSymbols
import java.time.LocalDateTime
import java.util
import scala.collection.mutable

class UserMainInfoRouter @Inject()
(
  api: API,
  auth: Auth
) {

  def createReport(user: Int, year: Int): File = {
    val data: mutable.Map[Int, util.Map[Int, Double]] = api.changesPerYear(user, year).asScala
    val tags = api.getUserTags(user).asScala.toSeq

    val months: Seq[String] = new DateFormatSymbols(new util.Locale("ru")).getShortMonths.toSeq
    val tmpFile: File = File.createTempFile(util.UUID.randomUUID().toString + LocalDateTime.now().toString, ".tmp")

    val writer = CSVWriter.open(tmpFile)

    val dataFilter: Int => List[Double] =
      id =>
        data.toSeq.sortBy(_._1)
        .map { case (_, d) => d.get(id) }
        .toList
        .zipAll(Array.fill(12)(0.0), 0.0, 0.0)
        .map { case (a, _) => a }

    writer.writeRow("" +: months :+ "Всего")

    writer.writeRow(Seq("Расходы:", ""))
    calculateAndWrite(dataFilter, tags.filter(_.kind == -1), writer)
    writer.writeRow("")

    writer.writeRow(Seq("Доходы:", ""))
    calculateAndWrite(dataFilter, tags.filter(_.kind == 1), writer)

    writer.writeRow("")

    writer.writeRow(Seq("Совместное:", ""))
    calculateAndWrite(dataFilter, tags.filter(_.kind == 0), writer)

    writer.flush()
    writer.close()

    tmpFile
  }

  private def calculateAndWrite(deltas: Int => List[Double], tags: Seq[Tag], writer: CSVWriter): Unit ={
    var totalMonths: List[Double] = List()

    for (t <- tags) {
      val tagDeltas = deltas(t.id)
      totalMonths = totalMonths.zipAll(tagDeltas, 0.0, 0.0).map { case (a, b) => a + b }
      val sum = tagDeltas.sum

      writer.writeRow(t.name +: tagDeltas :+ "" :+ sum)
    }

    writer.writeRow("Итого: " +: totalMonths :+ "" :+  totalMonths.sum)
  }

  def route: Route = {
    path("monthChanges") {
      pathEnd {
        (get & auth) {
          user => complete(gson.toJson(api.changesPerMonthByTags(user.id)))
        }
      }
    } ~ path("yearChanges") {
      pathEnd {
        (get & auth & parameters("year".as[Int])) {
          (user, year) => complete(gson.toJson(api.changesPerYear(user.id, year)))
        }
      }
    } ~ path("report") {
      pathEnd {
        (get & auth & parameters("year".as[Int])) {
          (user, year) => complete(HttpEntity.fromFile(MediaTypes.`text/csv`.toContentType(HttpCharsets.UTF_8), createReport(user.id, year)))
        }
      }
    }
  }
}
