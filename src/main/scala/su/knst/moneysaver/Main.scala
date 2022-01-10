package su.knst.moneysaver

import akka.actor.ActorSystem
import com.google.inject.{Binder, Guice, Module}
import http.HttpServer
import su.knst.moneysaver.utils.console.CommandHandler
import su.knst.moneysaver.utils.time.TaskTimes

import java.text.DateFormatSymbols
import java.time.LocalDateTime
import java.util
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.io.StdIn.readLine

object Main extends App {
  implicit val system = ActorSystem("main")

  try {
    val inj = Guice.createInjector(new Module {
      override def configure(binder: Binder): Unit = {
        binder.bind(classOf[ActorSystem]).toInstance(system)
      }
    })

    val server = inj.getInstance(classOf[HttpServer])

    val future = server.start()

    while (true){
      inj.getInstance(classOf[CommandHandler])(readLine())
    }

    Await.result(future.flatMap(_.whenTerminated)(system.dispatcher), Duration.Inf)
  }finally system.terminate();
}
