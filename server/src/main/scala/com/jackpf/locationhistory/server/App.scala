package com.jackpf.locationhistory.server

import com.jackpf.locationhistory.server.db.DataSourceFactory
import com.jackpf.locationhistory.server.enricher.{
  ConfiguredEnrichers,
  EnricherExecutor,
  OSMEnricher
}
import com.jackpf.locationhistory.server.grpc.{AuthenticationManager, Services}
import com.jackpf.locationhistory.server.model.StorageType
import com.jackpf.locationhistory.server.repo.*
import com.jackpf.locationhistory.server.service.{JwtAuthService, NotificationService, OSMService}
import com.jackpf.locationhistory.server.util.Logging
import scopt.OptionParser
import sttp.client4.DefaultFutureBackend

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*
import scala.concurrent.{Await, Future}

object App extends Logging {
  private val ApplicationName: String = "location-history"

  private val parser: OptionParser[Args] =
    new scopt.OptionParser[Args](ApplicationName) {
      head(ApplicationName)

      opt[Int]('b', "beacon-port")
        .valueName("<beacon-port>")
        .action((x, c) => c.copy(beaconPort = Some(x)))
        .withFallback(() => 8080)
        .text("Beacon service port (for Android clients)")

      opt[Int]('a', "admin-port")
        .valueName("<admin-port>")
        .action((x, c) => c.copy(adminPort = Some(x)))
        .withFallback(() => 8081)
        .text("Admin service port (for the web UI)")

      opt[String]('x', "admin-password")
        .valueName("<admin-password>")
        .action((x, c) => c.copy(adminPassword = Some(x)))
        .required()
        .text("Administrator password for admin endpoints")

      opt[String]('d', "data-directory")
        .valueName("<data-directory>")
        .action((x, c) => c.copy(dataDirectory = Some(x)))
        .withFallback(() => "/tmp")
        .text("Directory for data storage")

      opt[StorageType]('s', "storage-type")
        .valueName("<storage-type>")
        .action((x, c) => c.copy(storageType = Some(x)))
        .required()
        .text("Storage type for data")

      opt[String]("ssl-certs-directory")
        .valueName("<ssl-certs-directory>")
        .action((x, c) => c.copy(sslCertsDir = Some(x)))
        .withFallback(() => "/tmp/certs")
        .text("Path to SSL certificates")

      opt[Seq[String]]('e', "enrichers")
        .valueName("<enricher1,enricher2,...>")
        .action((x, c) => c.copy(enrichers = x))
        .withFallback(() => Seq.empty)
        .text("Comma separated list of enrichers")
    }

  def main(args: Array[String]): Unit = {
    val parsedArgs = parser
      .parse(args, Args())
      .getOrElse(sys.exit(1))

    val authenticationManager = new AuthenticationManager(parsedArgs.adminPassword.get)
    val tokenService = new JwtAuthService

    val dataSource = new DataSourceFactory(parsedArgs.dataDirectory.get, "database.db")
      .create(parsedArgs.storageType.get)
    val repoFactory = new RepoFactory(dataSource = dataSource)

    val deviceRepo = repoFactory.deviceRepo(parsedArgs.storageType.get)
    val locationRepo = repoFactory.locationRepo(parsedArgs.storageType.get)
    val sttpBackend = DefaultFutureBackend()
    val notificationService = new NotificationService(sttpBackend)

    // Available enrichers
    val enrichers = Seq(
      new OSMEnricher(new OSMService(sttpBackend))
    )
    val loadedEnrichers = ConfiguredEnrichers.fromConfigured(parsedArgs.enrichers, enrichers)
    log.info(s"Loaded enrichers: ${loadedEnrichers.map(_.name).mkString(", ")}")
    val enricherExecutor = new EnricherExecutor(loadedEnrichers)

    Await.result(
      Future.sequence(
        Seq(
          deviceRepo.init(),
          locationRepo.init()
        )
      ),
      1.minute
    )

    val beaconServer = new AppServer(
      "Beacon service",
      parsedArgs.beaconPort.get,
      parsedArgs.sslCertsPath,
      Services.beaconServices(deviceRepo, locationRepo, enricherExecutor)*
    ).start()

    val adminServer = new AppServer(
      "Admin service",
      parsedArgs.adminPort.get,
      sslCertsPath = None,
      Services.adminServices(
        authenticationManager,
        tokenService,
        deviceRepo,
        locationRepo,
        notificationService
      )*
    ).start()

    sys.addShutdownHook {
      beaconServer.shutdown()
      adminServer.shutdown()
      sttpBackend.close(): Unit
    }

    beaconServer.awaitTermination()
    adminServer.awaitTermination()
  }
}
