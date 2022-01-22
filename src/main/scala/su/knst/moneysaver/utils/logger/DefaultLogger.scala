package su.knst.moneysaver.utils.logger

import su.knst.moneysaver.utils.logger.DefaultLogger.{logFileOutput, stdout}

import java.io.{File, FileWriter, OutputStream}
import java.nio.file.{Files, Path, Paths}
import java.time.{Instant, LocalDate, LocalDateTime, ZoneId, ZoneOffset}
import java.time.format.DateTimeFormatter
import java.util.zip.GZIPOutputStream

class DefaultLogger(var prefixes: String*) extends TaggedLogger(prefixes:_*) {
  protected val console = new StreamLogger(stdout)
  protected val file = new StreamLogger(logFileOutput)

  override def log(text: String): Unit = {
    console.log(text)
    file.log(text)
  }

}

object DefaultLogger {
  val logFileOutput: OutputStream = {
    val historyDir: Path = Paths.get("./logs/history/")

    if (!Files.exists(historyDir))
      Files.createDirectories(historyDir)

    val lastLog = Paths.get("./logs/last.log")

    if (Files.exists(lastLog)){
      val newName = LocalDateTime
        .ofInstant(Instant.ofEpochSecond(lastLog.toFile.lastModified() / 1000), ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("dd-MM-yyyy_hh-mm")) + ".zip"

      val stream = new GZIPOutputStream(Files.newOutputStream(Paths.get(s"./logs/history/$newName")))

      Files.copy(lastLog, stream)
      stream.flush()
      stream.finish()
      stream.close()
    }

    Files.newOutputStream(lastLog)
  }

  val stdout: OutputStream = Files.newOutputStream(Paths.get("/dev/stdout"))

  def apply(prefixes: String*) : DefaultLogger = new DefaultLogger(prefixes:_*)
}
