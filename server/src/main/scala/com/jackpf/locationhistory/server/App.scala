package com.jackpf.locationhistory.server

import beacon.beacon_service.BeaconServiceGrpc
import com.jackpf.locationhistory.server.grpc.BeaconServiceImpl
import com.jackpf.locationhistory.server.repo.{
  DeviceRepo,
  InMemoryDeviceRepo,
  InMemoryLocationRepo,
  LocationRepo
}
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

    val deviceRepo: DeviceRepo = new InMemoryDeviceRepo
    val locationRepo: LocationRepo = new InMemoryLocationRepo

    new AppServer(
      parsedArgs,
      BeaconServiceGrpc.bindService(
        new BeaconServiceImpl(deviceRepo, locationRepo),
        global
      )
    ).listen().awaitTermination()
  }
}
