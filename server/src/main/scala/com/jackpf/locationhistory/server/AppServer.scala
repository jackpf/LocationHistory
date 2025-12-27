package com.jackpf.locationhistory.server

import com.jackpf.locationhistory.server.grpc.interceptors.LoggingInterceptor
import com.jackpf.locationhistory.server.grpc.interceptors.LoggingInterceptor.LogLevel
import com.jackpf.locationhistory.server.util.Logging
import io.grpc.health.v1.HealthCheckResponse.ServingStatus
import io.grpc.{Server, ServerBuilder, ServerServiceDefinition}

import scala.jdk.CollectionConverters.*
import io.grpc.protobuf.services.HealthStatusManager

class AppServer(
    port: Int,
    services: ServerServiceDefinition*
) extends Logging {
  private def healthManager: HealthStatusManager = {
    val healthManager = new HealthStatusManager()
    healthManager.setStatus("", ServingStatus.SERVING)
    healthManager
  }

  def start(): Server = {
    log.info(s"Listening on port ${port}")

    ServerBuilder
      .forPort(port)
      .intercept(new LoggingInterceptor(LogLevel.INFO))
      .addService(healthManager.getHealthService)
      .addServices(services.asJava)
      .build()
      .start()
  }
}
