package su.knst.moneysaver.utils.logger

import java.io.OutputStream
import java.time.Instant
import java.util.Date

abstract class AbstractLogger extends OutputStream {
  protected var buffer : Array[Int] = Array()

  def log(text: String): Unit



  def write(text: String): Unit =
    buffer = buffer ++ text.getBytes().map(_.toInt)

  def write(var1: Int): Unit =
    buffer = buffer :+ var1

  override def write(b: Array[Byte], off: Int, len: Int): Unit =
    write(new String(b.slice(off, off + len)))

  override def flush() : Unit = {
    if (buffer.length > 0)
      log(new String(buffer.map(_.toChar)))

    buffer = Array()
  }



  protected def time: String = String.valueOf(Date.from(Instant.now))

  protected def formatPrefixes(prefixes: String*) : String =
    prefixes.map(s => s"[${s.toUpperCase()}]").mkString(" ")

  protected def formatMessage(text: String, prefixes: String*) : String =
    s"${formatPrefixes(prefixes:_*)} $text"
}
