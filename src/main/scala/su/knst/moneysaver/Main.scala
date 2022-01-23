package su.knst.moneysaver

import akka.actor.ActorSystem
import com.google.inject.{Binder, Guice, Module}
import http.HttpServer
import org.bouncycastle.jce.provider.BouncyCastleProvider
import su.knst.moneysaver.utils.console.CommandHandler
import su.knst.moneysaver.utils.logger.{AbstractLogger, DefaultLogger, StreamLogger}

import java.io.{InputStream, OutputStream, PrintStream}
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.io.StdIn.readLine

object Main extends App {
  implicit val system = ActorSystem("main")
  val logger = DefaultLogger("main")
  val fileLogger = new StreamLogger(DefaultLogger.logFileOutput)
  import java.security.Security

  Security.addProvider(new BouncyCastleProvider)

  try {
    val systemLogger: AbstractLogger = new DefaultLogger("system")

    System.setOut(
      new PrintStream(systemLogger, true)
    )

    System.setErr(
      new PrintStream(systemLogger, true)
    )

    logger.info("Init Guice")
    val inj = Guice.createInjector(new Module {
      override def configure(binder: Binder): Unit = {
        binder.bind(classOf[ActorSystem]).toInstance(system)
      }
    })

    logger.info("Init http server")
    val server = inj.getInstance(classOf[HttpServer])

    logger.info("Starting server")
    val future = server.start()

    while (true){
      try {
        print("> ")
        val line = readLine()
        fileLogger.log(line + "\n")

        inj.getInstance(classOf[CommandHandler])(line)
      }catch {
        case e: Exception => {
          logger.error("Error to process command:")
          e.printStackTrace()
        }
      }
    }

    Await.result(future.flatMap(_.whenTerminated)(system.dispatcher), Duration.Inf)
  }finally system.terminate()
}
