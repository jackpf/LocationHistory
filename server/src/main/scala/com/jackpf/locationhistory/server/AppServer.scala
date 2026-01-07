package com.jackpf.locationhistory.server

import com.jackpf.locationhistory.server.grpc.interceptors.LoggingInterceptor
import com.jackpf.locationhistory.server.grpc.interceptors.LoggingInterceptor.LogLevel
import com.jackpf.locationhistory.server.util.{Logging, SSLUtils}
import io.grpc.health.v1.HealthCheckResponse.ServingStatus
import io.grpc.protobuf.services.HealthStatusManager
import io.grpc.{Server, ServerBuilder, ServerServiceDefinition}

import java.nio.file.{Path, Paths}
import scala.jdk.CollectionConverters.*

class AppServer(
    port: Int,
    sslCertsPath: Option[Path],
    services: ServerServiceDefinition*
) extends Logging {
  private val serverCrtFile: Path = Paths.get("server.crt")
  private val serverPemFile: Path = Paths.get("server.pem")

  private def healthManager: HealthStatusManager = {
    val healthManager = new HealthStatusManager()
    healthManager.setStatus("", ServingStatus.SERVING)
    healthManager
  }

  private def addSslAuthMaybe[T <: ServerBuilder[T]](
      builder: ServerBuilder[T]
  ): ServerBuilder[T] = {
    sslCertsPath match {
      case Some(path) =>
        builder.useTransportSecurity(
          path.resolve(serverCrtFile).toFile,
          path.resolve(serverPemFile).toFile
        )
      case None =>
        builder
    }
  }

  def start(): Server = {
    sslCertsPath match {
      case Some(path) =>
        log.info("Using SSL")
        println(
          SSLUtils.sslFingerprint(path.resolve(serverCrtFile).toFile)
        )
      case None =>
        log.warn("Not using SSL")
    }

    log.info(s"Listening on port ${port}")

    addSslAuthMaybe(
      ServerBuilder
        .forPort(port)
        .intercept(new LoggingInterceptor(LogLevel.INFO))
        .addService(healthManager.getHealthService)
        .addServices(services.asJava)
    )
      .build()
      .start()
  }
}
