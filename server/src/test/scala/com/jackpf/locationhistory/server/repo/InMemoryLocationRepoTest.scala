package com.jackpf.locationhistory.server.repo

import com.jackpf.locationhistory.server.model.{Device, DeviceId, Location}
import com.jackpf.locationhistory.server.testutil.{
  DefaultScope,
  DefaultSpecification
}
import org.specs2.concurrent.ExecutionEnv

import scala.concurrent.Future
import scala.util.Try

class InMemoryLocationRepoTest(implicit ee: ExecutionEnv)
    extends DefaultSpecification {
  trait Context extends DefaultScope {
    val locationRepo: LocationRepo = new InMemoryLocationRepo
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

    "get empty locations by device" >> in(new StoredLocationContext {}) {
      context =>
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
  }
}
