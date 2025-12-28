package com.jackpf.locationhistory.server

import com.jackpf.locationhistory.server.grpc.Services
import com.jackpf.locationhistory.server.repo.{
  DeviceRepo,
  InMemoryDeviceRepo,
  InMemoryLocationRepo,
  LocationRepo
}
import scopt.OptionParser

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

      opt[String]('x', "admin-password")
        .valueName("<admin-password>")
        .action((x, c) => c.copy(adminPassword = Some(x)))
        .required()
        .text(
          "Administrator password for admin endpoints"
        )
    }

  def main(args: Array[String]): Unit = {
    val parsedArgs = parser
      .parse(args, Args())
      .getOrElse(sys.exit(1))

    val deviceRepo: DeviceRepo = new InMemoryDeviceRepo
    val locationRepo: LocationRepo = new InMemoryLocationRepo

    new AppServer(
      parsedArgs.listenPort.get,
      Services(parsedArgs.adminPassword.get, deviceRepo, locationRepo)*
    ).start().awaitTermination()
  }
}
