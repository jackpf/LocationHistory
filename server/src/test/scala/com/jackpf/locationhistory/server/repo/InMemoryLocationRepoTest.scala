package com.jackpf.locationhistory.server.repo

import com.jackpf.locationhistory.server.model.{Device, DeviceId, Location}
import com.jackpf.locationhistory.server.testutil.{DefaultScope, DefaultSpecification}
import org.specs2.concurrent.ExecutionEnv

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
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
      Location(timestamp = 123L, lat = 0.1, lon = 0.2, accuracy = 0.3)

    lazy val result: Future[Try[Unit]] =
      locationRepo.storeDeviceLocation(device.id, location)
  }

  "In memory location repo" should {
    "store a device location" >> in(new StoredLocationContext {}) { context =>
      context.result must beSuccessfulTry.await
    }

    "get locations by device" >> in(new StoredLocationContext {}) { context =>
      context.result must beSuccessfulTry.await

      context.locationRepo
        .getForDevice(context.device.id) must beEqualTo(
        Seq(context.location)
      ).await
    }

    "get empty locations by device" >> in(new StoredLocationContext {}) { context =>
      context.result must beSuccessfulTry.await

      context.locationRepo
        .getForDevice(DeviceId("non-existing")) must beEmpty[
        Seq[Location]
      ].await
    }

    "delete all locations" >> in(new StoredLocationContext {}) { context =>
      context.result must beSuccessfulTry.await

      context.locationRepo.deleteAll() must beEqualTo(()).await

      context.locationRepo
        .getForDevice(context.device.id) must beEmpty[Seq[Location]].await
    }

    "limit items per device" >> in(new Context {
      override lazy val maxItemsPerDevice: Long = 4
    }) { context =>
      val deviceId = DeviceId("123")

      def storeLocation(ts: Long) = {
        Await.result(
          context.locationRepo.storeDeviceLocation(
            deviceId,
            Location(timestamp = ts, lat = 0.1, lon = 0.2, accuracy = 0.3)
          ),
          Duration.Inf
        )
      }

      storeLocation(1L)
      storeLocation(2L)
      storeLocation(3L)
      storeLocation(4L)
      storeLocation(5L)
      storeLocation(6L)

      context.locationRepo.getForDevice(deviceId) must haveSize(4).await
      context.locationRepo.getForDevice(deviceId).map(_.map(_.timestamp)) must beEqualTo(
        Seq(
          3L,
          4L,
          5L,
          6L
        )
      ).await
    }
  }
}
