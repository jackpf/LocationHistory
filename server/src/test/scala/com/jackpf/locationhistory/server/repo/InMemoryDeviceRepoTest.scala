package com.jackpf.locationhistory.server.repo

import com.jackpf.locationhistory.server.model.StoredDevice.DeviceStatus
import com.jackpf.locationhistory.server.model.{Device, DeviceId, StoredDevice}
import com.jackpf.locationhistory.server.testutil.DefaultSuite
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Failure, Success, Try}

trait Context {
  val deviceRepo: DeviceRepo = new InMemoryDeviceRepo()
}

trait NoDevicesContext extends Context

trait OneDeviceContext extends Context {
  lazy val device: Device = Device(id = DeviceId("123"), publicKey = "xxx")
  val registerResult: Try[Unit] =
    deviceRepo.register(device).futureValue
}

class InMemoryDeviceRepoTest extends DefaultSuite {
  test("get a non-existing device") {
    new NoDevicesContext {
      val result: Option[StoredDevice] =
        deviceRepo.get(DeviceId("non-existing")).futureValue

      result mustBe None
    }
  }

  test("get all on empty devices") {
    new NoDevicesContext {
      val result: Seq[StoredDevice] = deviceRepo.getAll.futureValue

      result mustBe Seq.empty
    }
  }

  test("register a single device") {
    new OneDeviceContext {
      registerResult mustBe Success[Unit](())
    }
  }

  test("get a single device") {
    new OneDeviceContext {
      registerResult mustBe Success[Unit](())

      val getResult: Option[StoredDevice] =
        deviceRepo.get(device.id).futureValue

      getResult mustBe Some(
        StoredDevice(device = device, status = DeviceStatus.Pending)
      )
    }
  }

  test("get all with single device") {
    new OneDeviceContext {
      registerResult mustBe Success[Unit](())

      val getAllResult: Seq[StoredDevice] =
        deviceRepo.getAll.futureValue

      getAllResult mustBe Seq(
        StoredDevice(device = device, status = DeviceStatus.Pending)
      )
    }
  }

  test("fail on registering an existing device") {
    new OneDeviceContext {
      registerResult mustBe Success[Unit](())

      val registerResult2: Try[Unit] = deviceRepo.register(device).futureValue
      registerResult2 mustBe Failure[Unit](
        new IllegalArgumentException(
          "Device Device(123,xxx) is already registered"
        )
      )
    }
  }
}
