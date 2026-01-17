package com.jackpf.locationhistory.server

import com.jackpf.locationhistory.server.db.DataSourceFactory
import com.jackpf.locationhistory.server.grpc.interceptors.TokenService
import com.jackpf.locationhistory.server.grpc.{AuthenticationManager, Services}
import com.jackpf.locationhistory.server.model.StorageType
import com.jackpf.locationhistory.server.repo.*
import com.jackpf.locationhistory.server.service.{JwtAuthService, NotificationService}
import scopt.OptionParser
import sttp.client4.DefaultFutureBackend

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*

object App {
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
    }

  def main(args: Array[String]): Unit = {
    val parsedArgs = parser
      .parse(args, Args())
      .getOrElse(sys.exit(1))

    val authenticationManager = new AuthenticationManager(parsedArgs.adminPassword.get)
    val tokenService: TokenService = new JwtAuthService

    val dataSource = new DataSourceFactory(parsedArgs.dataDirectory.get, "database.db")
      .create(parsedArgs.storageType.get)
    val repoFactory: RepoFactory = new RepoFactory(dataSource = dataSource)

    val deviceRepo: DeviceRepo = repoFactory.deviceRepo(parsedArgs.storageType.get)
    val locationRepo: LocationRepo = repoFactory.locationRepo(parsedArgs.storageType.get)
    val sttpBackend = DefaultFutureBackend()
    val notificationService: NotificationService = new NotificationService(sttpBackend)

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
      Services.beaconServices(deviceRepo, locationRepo)*
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
