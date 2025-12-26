package com.jackpf.locationhistory.server

import com.jackpf.locationhistory.server.util.Logging
import io.grpc.{Server, ServerBuilder, ServerServiceDefinition}

import scala.jdk.CollectionConverters.*

class AppServer(
    port: Int,
    services: ServerServiceDefinition*
) extends Logging {
  def start(): Server = {
    log.info(s"Listening on port ${port}")

    ServerBuilder
      .forPort(port)
      .addServices(services.asJava)
      .build()
      .start()
  }
}
