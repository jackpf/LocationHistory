package com.jackpf.locationhistory.server.testutil

import com.jackpf.locationhistory.server.AppServer
import com.jackpf.locationhistory.server.grpc.Services
import com.jackpf.locationhistory.server.repo.{
  DeviceRepo,
  InMemoryDeviceRepo,
  InMemoryLocationRepo,
  LocationRepo
}
import io.grpc.Server

import java.util.concurrent.TimeUnit
import scala.compiletime.uninitialized
import scala.concurrent.ExecutionContext.Implicits.global

object TestServer {
  val TestPort = 8080

  val deviceRepo: DeviceRepo = new InMemoryDeviceRepo
  val locationRepo: LocationRepo = new InMemoryLocationRepo

  var server: Server = uninitialized

  def start(): Unit = synchronized {
    if (server == null) {
      println("Starting server")

      server = new AppServer(
        TestPort,
        Services(deviceRepo, locationRepo)*
      ).start()
    }
  }

  def stop(): Unit = synchronized {
    if (server != null) {
      println("Stopping server")
      server.shutdown().awaitTermination(10, TimeUnit.SECONDS): Unit
    }
  }

  sys.addShutdownHook {
    stop()
  }
}
