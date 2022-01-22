package su.knst.moneysaver.utils.logger

import java.io.{File, FileWriter, OutputStream}
import java.nio.file.{Files, Path, Paths}
import java.time.{Instant, LocalDate, LocalDateTime, ZoneId, ZoneOffset}
import java.time.format.DateTimeFormatter
import java.util.zip.GZIPOutputStream

abstract class TaggedLogger(prefixes: String*) extends AbstractLogger {
  def info(text: String, subPrefixes: String*): Unit =
    log(formatMessage(text + "\n", time +: "info" +: (prefixes ++ subPrefixes):_*))

  def warn(text: String): Unit =
    log(formatMessage(text + "\n", time +: "warn" +: prefixes:_*))

  def error(text: String): Unit =
    log(formatMessage(text + "\n", time +: "error" +: prefixes:_*))
}
