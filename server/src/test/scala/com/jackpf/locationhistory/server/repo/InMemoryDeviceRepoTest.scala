package com.jackpf.locationhistory.server.repo

import com.jackpf.locationhistory.server.grpc.GrpcResponse.GrpcResponse
import com.jackpf.locationhistory.server.model.StoredDevice.DeviceStatus
import com.jackpf.locationhistory.server.model.{Device, DeviceId, StoredDevice}
import com.jackpf.locationhistory.server.testutil.{
  DefaultScope,
  DefaultSpecification
}
import io.grpc.Status
import io.grpc.Status.Code
import org.specs2.collection.IsEmpty
import org.specs2.concurrent.ExecutionEnv

import scala.concurrent.Future

class InMemoryDeviceRepoTest(implicit ee: ExecutionEnv)
    extends DefaultSpecification {
  trait Context extends DefaultScope {
    val deviceRepo: DeviceRepo = new InMemoryDeviceRepo()
  }

  trait NoDevicesContext extends Context

  trait OneDeviceContext extends Context {
    lazy val device: Device = Device(id = DeviceId("123"), publicKey = "xxx")
    val registerResult: Future[GrpcResponse[Unit]] =
      deviceRepo.register(device)
  }

  "In memory device repo" should {
    "get a non-existing device" >> in(new NoDevicesContext {}) { context =>
      val result: Future[Option[StoredDevice]] =
        context.deviceRepo.get(DeviceId("non-existing"))

      result must beNone.await
    }

    "get all on empty devices" >> in(new NoDevicesContext {}) { context =>
      val result: Future[Seq[StoredDevice]] = context.deviceRepo.getAll

      result must beEmpty[Seq[StoredDevice]].await
    }

    "register a single device" >> in(new OneDeviceContext {}) { context =>
      context.registerResult must beRight.await
    }

    "get a single device" >> in(new OneDeviceContext {}) { context =>
      context.registerResult must beRight.await

      val getResult: Future[Option[StoredDevice]] =
        context.deviceRepo.get(context.device.id)

      getResult must beSome(
        StoredDevice(device = context.device, status = DeviceStatus.Pending)
      ).await
    }

    "get all with a single device" >> in(new OneDeviceContext {}) { context =>
      context.registerResult must beRight.await

      val getAllResult: Future[Seq[StoredDevice]] =
        context.deviceRepo.getAll

      getAllResult must beEqualTo(
        Seq(
          StoredDevice(device = context.device, status = DeviceStatus.Pending)
        )
      ).await
    }

    "fail on registering an existing device" >> in(new OneDeviceContext {}) {
      context =>
        context.registerResult must beRight.await

        val registerResult2: Future[GrpcResponse[Unit]] =
          context.deviceRepo.register(context.device)

        registerResult2 must beLeft[Status].like { case e =>
          e.getCode === Code.INVALID_ARGUMENT
          e.getDescription === "Device 123 is already registered"
        }.await
    }
  }
}
