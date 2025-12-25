package com.jackpf.locationhistory.server

import beacon.beacon_service.BeaconServiceGrpc
import com.jackpf.locationhistory.server.grpc.BeaconServiceImpl
import com.jackpf.locationhistory.server.util.Logging
import io.grpc.{Server, ServerBuilder}

import scala.concurrent.ExecutionContext

class AppServer(args: Args) extends Logging {
  def listen()(using ec: ExecutionContext): Server = {
    log.info(s"Listening on port ${args.listenPort.get}")

    ServerBuilder
      .forPort(args.listenPort.get)
      .addService(
        BeaconServiceGrpc.bindService(new BeaconServiceImpl, ec)
      )
      .build()
      .start()
  }
}
