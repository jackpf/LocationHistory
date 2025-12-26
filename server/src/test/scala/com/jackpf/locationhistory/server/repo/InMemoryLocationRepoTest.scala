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
import com.jackpf.locationhistory.server.util.GrpcResponse.GrpcTry
import io.grpc.Status
import io.grpc.Status.Code
import org.specs2.concurrent.ExecutionEnv

import scala.concurrent.Future

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

    lazy val result: Future[GrpcTry[Unit]] =
      locationRepo.storeDeviceLocation(storedDevice, location)
  }

  "In memory location repo" should {
    "store a device location" >> in(new StoredLocationContext {}) { context =>
      context.result must beRight.await
    }

    "fail if storing device location for pending device" >> in(
      new StoredLocationContext {
        override lazy val storedDevice: StoredDevice =
          StoredDevice(device, DeviceStatus.Pending)
      }
    ) { context =>
      context.result must beLeft[Status].like { case e =>
        e.getCode === Code.INVALID_ARGUMENT
        e.getDescription === "Device 123 is not registered"
      }.await
    }
  }
}
