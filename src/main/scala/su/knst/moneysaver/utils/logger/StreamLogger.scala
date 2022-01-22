package su.knst.moneysaver.utils.logger

import java.io.{BufferedOutputStream, BufferedWriter, File, FileWriter, OutputStream}
import java.nio.charset.Charset
import java.nio.file.{Files, Path, StandardOpenOption}
import java.util.Objects


class StreamLogger(stream: OutputStream) extends AbstractLogger {
  protected val bufferedStream : BufferedOutputStream = new BufferedOutputStream(stream)

  override def write(text: String): Unit =
    bufferedStream.write(text.getBytes())

  override def write(var1: Int): Unit =
    bufferedStream.write(var1)

  override def write(b: Array[Byte], off: Int, len: Int): Unit =
    bufferedStream.write(b, off, len)

  override def flush() : Unit = bufferedStream.flush()

  override def log(text: String): Unit = {
    write(text)
    flush()
  }
}
