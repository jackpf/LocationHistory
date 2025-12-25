package com.jackpf.locationhistory.server

import scopt.OptionParser

import scala.concurrent.ExecutionContext.Implicits.global

object App {
  private val ApplicationName: String = "location-history"

  private val parser: OptionParser[Args] =
    new scopt.OptionParser[Args](ApplicationName) {
      head(ApplicationName)

      opt[Int]('p', "listen-port")
        .valueName("<listen-port>")
        .action((x, c) => c.copy(listenPort = Some(x)))
        .withFallback(() => 8080)
        .text(
          "Port to listen on"
        )
    }

  def main(args: Array[String]): Unit = {
    val parsedArgs = parser
      .parse(args, Args())
      .getOrElse(throw new IllegalStateException("No config"))

    new AppServer(parsedArgs).listen().awaitTermination()
  }
}
