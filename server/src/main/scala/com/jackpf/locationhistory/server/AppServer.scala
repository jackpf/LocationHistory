package com.jackpf.locationhistory.server

import com.jackpf.locationhistory.server.util.Logging
import io.grpc.{Server, ServerBuilder, ServerServiceDefinition}

import scala.jdk.CollectionConverters.*

class AppServer(
    args: Args,
    services: ServerServiceDefinition*
) extends Logging {
  def listen(): Server = {
    log.info(s"Listening on port ${args.listenPort.get}")

    ServerBuilder
      .forPort(args.listenPort.get)
      .addServices(services.asJava)
      .build()
      .start()
  }
}
