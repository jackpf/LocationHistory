package com.jackpf.locationhistory.server

import com.jackpf.locationhistory.server.db.DataSourceFactory
import com.jackpf.locationhistory.server.grpc.{AuthenticationManager, Services}
import com.jackpf.locationhistory.server.model.StorageType
import com.jackpf.locationhistory.server.repo.*
import scopt.OptionParser

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.*

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

      opt[String]('x', "admin-password")
        .valueName("<admin-password>")
        .action((x, c) => c.copy(adminPassword = Some(x)))
        .required()
        .text(
          "Administrator password for admin endpoints"
        )

      opt[String]('d', "data-directory")
        .valueName("<data-directory>")
        .action((x, c) => c.copy(dataDirectory = Some(x)))
        .withFallback(() => "/tmp")
        .text(
          "Directory for data storage"
        )

      opt[StorageType]('s', "storage-type")
        .valueName("<storage-type>")
        .action((x, c) => c.copy(storageType = Some(x)))
        .required()
        .text(
          "Storage type for data"
        )
    }

  def main(args: Array[String]): Unit = {
    val parsedArgs = parser
      .parse(args, Args())
      .getOrElse(sys.exit(1))

    val authenticationManager = new AuthenticationManager(parsedArgs.adminPassword.get)

    val dataSource = new DataSourceFactory(parsedArgs.dataDirectory.get, "database.db")
      .create(parsedArgs.storageType.get)
    val repoFactory: RepoFactory = new RepoFactory(dataSource = dataSource)

    val deviceRepo: DeviceRepo = repoFactory.deviceRepo(parsedArgs.storageType.get)
    val locationRepo: LocationRepo = repoFactory.locationRepo(parsedArgs.storageType.get)

    Await.result(
      Future.sequence(
        Seq(
          deviceRepo.init(),
          locationRepo.init()
        )
      ),
      1.minute
    )

    new AppServer(
      parsedArgs.listenPort.get,
      Services(authenticationManager, deviceRepo, locationRepo)*
    ).start().awaitTermination()
  }
}
