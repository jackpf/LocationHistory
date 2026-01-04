package com.jackpf.locationhistory.server.repo

import com.jackpf.locationhistory.server.model.{DeviceId, Location}
import org.specs2.concurrent.ExecutionEnv

import scala.concurrent.Future
import scala.util.Try

class InMemoryLocationRepoTest(implicit ee: ExecutionEnv) extends LocationRepoTest {
  override def createLocationRepo: LocationRepo = {
    lazy val maxItemsPerDevice: Long = 4
    new InMemoryLocationRepo(maxItemsPerDevice)
  }

  "In memory location repo" should {
    "limit items per device" >> in(new Context {}) { context =>
      val deviceId = DeviceId("123")

      def storeLocation(ts: Long): Future[Try[Unit]] =
        context.locationRepo.storeDeviceLocation(
          deviceId,
          Location(lat = 0.1, lon = 0.2, accuracy = 0.3),
          ts
        )

      {
        for {
          _ <- storeLocation(1L)
          _ <- storeLocation(2L)
          _ <- storeLocation(3L)
          _ <- storeLocation(4L)
          _ <- storeLocation(5L)
          _ <- storeLocation(6L)
          locations <- context.locationRepo.getForDevice(deviceId)
        } yield {
          locations must haveSize(4)
          locations.map(_.timestamp) must beEqualTo(
            Seq(
              3L,
              4L,
              5L,
              6L
            )
          )
        }
      }.await
    }
  }
}
