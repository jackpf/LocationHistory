package com.jackpf.locationhistory.server.repo

import com.jackpf.locationhistory.server.model.{Device, DeviceId, Location, StoredLocation}
import com.jackpf.locationhistory.server.testutil.{DefaultScope, DefaultSpecification}
import org.specs2.concurrent.ExecutionEnv

import scala.concurrent.Future
import scala.util.Try

class InMemoryLocationRepoTest(implicit ee: ExecutionEnv) extends DefaultSpecification {
  trait Context extends DefaultScope {
    lazy val maxItemsPerDevice: Long = InMemoryLocationRepo.DefaultMaxItemsPerDevice
    val locationRepo: LocationRepo = new InMemoryLocationRepo(maxItemsPerDevice)
  }

  trait StoredLocationContext extends Context {
    lazy val device =
      Device(id = DeviceId("123"), publicKey = "xxx")
    lazy val location =
      Location(lat = 0.1, lon = 0.2, accuracy = 0.3)
    lazy val timestamp: Long = 123L
    lazy val expectedStoredLocation = StoredLocation(location, timestamp)

    lazy val result: Future[Try[Unit]] =
      locationRepo.storeDeviceLocation(device.id, location, timestamp)
  }

  "In memory location repo" should {
    "store a device location" >> in(new StoredLocationContext {}) { context =>
      context.result must beSuccessfulTry.await
    }

    "get locations by device" >> in(new StoredLocationContext {}) { context =>
      context.result must beSuccessfulTry.await

      context.locationRepo
        .getForDevice(context.device.id) must beEqualTo(
        Seq(context.expectedStoredLocation)
      ).await
    }

    "get empty locations by device" >> in(new StoredLocationContext {}) { context =>
      context.result must beSuccessfulTry.await

      context.locationRepo
        .getForDevice(DeviceId("non-existing")) must beEmpty[
        Seq[StoredLocation]
      ].await
    }

    "delete all locations" >> in(new StoredLocationContext {}) { context =>
      context.result must beSuccessfulTry.await

      context.locationRepo.deleteAll() must beEqualTo(()).await

      context.locationRepo
        .getForDevice(context.device.id) must beEmpty[Seq[StoredLocation]].await
    }

    "limit items per device" >> in(new Context {
      override lazy val maxItemsPerDevice: Long = 4
    }) { context =>
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
