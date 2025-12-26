package com.jackpf.locationhistory.server.repo

import com.jackpf.locationhistory.server.model.StoredDevice.DeviceStatus
import com.jackpf.locationhistory.server.model.{
  Device,
  DeviceId,
  Location,
  StoredDevice
}
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
    lazy val storedDevice =
      StoredDevice(device = device, status = DeviceStatus.Registered)
    lazy val location =
      Location(timestamp = 123L, lat = 0.1, lon = 0.2, accuracy = 0.3)

    lazy val result: Future[Try[Unit]] =
      locationRepo.storeDeviceLocation(storedDevice, location)
  }

  "In memory location repo" should {
    "store a device location" >> in(new StoredLocationContext {}) { context =>
      context.result must beSuccessfulTry.await
    }

    "fail if storing device location for pending device" >> in(
      new StoredLocationContext {
        override lazy val storedDevice: StoredDevice =
          StoredDevice(device, DeviceStatus.Pending)
      }
    ) { context =>
      context.result must beFailedTry.like { case e: IllegalArgumentException =>
        e.getMessage must beEqualTo(
          "Device 123 is not registered"
        )
      }.await
    }
  }
}
