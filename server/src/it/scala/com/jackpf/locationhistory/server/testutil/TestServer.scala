package com.jackpf.locationhistory.server.testutil

import com.jackpf.locationhistory.server.AppServer
import com.jackpf.locationhistory.server.grpc.{AuthenticationManager, Services}
import com.jackpf.locationhistory.server.repo.{DeviceRepo, LocationRepo}
import io.grpc.Server

import java.util.concurrent.TimeUnit
import scala.compiletime.uninitialized

object TestServer {
  val TestPort = 8080
  val TestAdminPassword = "test"

  var server: Server = uninitialized

  def start(deviceRepo: DeviceRepo, locationRepo: LocationRepo): Unit =
    synchronized {
      if (server == null) {
        server = new AppServer(
          TestPort,
          sslCertsPath = None,
          Services(new AuthenticationManager(TestAdminPassword), deviceRepo, locationRepo)*
        ).start()
      }
    }

  def stop(): Unit = synchronized {
    if (server != null) {
      server.shutdown().awaitTermination(10, TimeUnit.SECONDS): Unit
    }
  }

  sys.addShutdownHook {
    stop()
  }
}
