package com.jackpf.locationhistory.server.repo

import com.jackpf.locationhistory.server.model.{Device, DeviceId, Location, StoredLocation}
import com.jackpf.locationhistory.server.testutil.{DefaultScope, DefaultSpecification}
import org.specs2.concurrent.ExecutionEnv

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.Try

abstract class LocationRepoTest(implicit ee: ExecutionEnv) extends DefaultSpecification {
  def createLocationRepo: LocationRepo

  trait Context extends DefaultScope {
    val locationRepo: LocationRepo = createLocationRepo
    Await.result(locationRepo.init(), Duration.Inf)
  }

  trait StoredLocationContext extends Context {
    lazy val device: Device =
      Device(id = DeviceId("123"), publicKey = "xxx")
    lazy val location: Location =
      Location(lat = 0.1, lon = 0.2, accuracy = 0.3)
    lazy val timestamp: Long = 123L
    lazy val expectedStoredLocation: StoredLocation = StoredLocation(location, timestamp)

    lazy val result: Future[Try[Unit]] =
      locationRepo.storeDeviceLocation(device.id, location, timestamp)
  }

  "Location repo" should {
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
  }
}
