package com.jackpf.locationhistory.server.repo

import com.jackpf.locationhistory.server.errors.ApplicationErrors.{
  DeviceAlreadyRegisteredException,
  DeviceNotFoundException
}
import com.jackpf.locationhistory.server.model.StoredDevice.DeviceStatus
import com.jackpf.locationhistory.server.model.{Device, DeviceId, StoredDevice}
import com.jackpf.locationhistory.server.testutil.{
  DefaultScope,
  DefaultSpecification
}
import org.specs2.collection.IsEmpty
import org.specs2.concurrent.ExecutionEnv

import scala.concurrent.Future
import scala.util.Try

class InMemoryDeviceRepoTest(implicit ee: ExecutionEnv)
    extends DefaultSpecification {
  trait Context extends DefaultScope {
    val deviceRepo: DeviceRepo = new InMemoryDeviceRepo()
  }

  trait NoDevicesContext extends Context

  trait OneDeviceContext extends Context {
    lazy val device: Device = Device(id = DeviceId("123"), publicKey = "xxx")
    val registerResult: Future[Try[Unit]] =
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

    "fail on deleting a non-existing device" >> in(new NoDevicesContext {}) {
      context =>
        val result: Future[Try[Unit]] =
          context.deviceRepo.delete(DeviceId("non-existing"))

        result must beFailedTry.like { case e: DeviceNotFoundException =>
          e.getMessage === "Device non-existing does not exist"
        }.await
    }

    "delete all on empty devices" >> in(new NoDevicesContext {}) { context =>
      val result: Future[Unit] = context.deviceRepo.deleteAll()

      result must beEqualTo(()).await
      context.deviceRepo.getAll must beEmpty[Seq[StoredDevice]].await
    }

    "register a single device" >> in(new OneDeviceContext {}) { context =>
      context.registerResult must beSuccessfulTry.await
    }

    "get a single device" >> in(new OneDeviceContext {}) { context =>
      context.registerResult must beSuccessfulTry.await

      val getResult: Future[Option[StoredDevice]] =
        context.deviceRepo.get(context.device.id)

      getResult must beSome(
        StoredDevice(device = context.device, status = DeviceStatus.Pending)
      ).await
    }

    "get all with a single device" >> in(new OneDeviceContext {}) { context =>
      context.registerResult must beSuccessfulTry.await

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
        context.registerResult must beSuccessfulTry.await

        val registerResult2: Future[Try[Unit]] =
          context.deviceRepo.register(context.device)

        registerResult2 must beFailedTry.like {
          case e: DeviceAlreadyRegisteredException =>
            e.getMessage === "Device 123 is already registered"
        }.await
    }

    "delete a device" >> in(new OneDeviceContext {}) { context =>
      context.registerResult must beSuccessfulTry.await

      val deleteResult: Future[Try[Unit]] =
        context.deviceRepo.delete(context.device.id)

      deleteResult must beSuccessfulTry.await
      context.deviceRepo.get(context.device.id) must beNone.await
      context.deviceRepo.getAll must beEmpty[Seq[StoredDevice]].await
    }

    "delete all devices" >> in(new OneDeviceContext {}) { context =>
      context.registerResult must beSuccessfulTry.await

      val result: Future[Unit] = context.deviceRepo.deleteAll()

      result must beEqualTo(()).await
      context.deviceRepo.getAll must beEmpty[Seq[StoredDevice]].await
    }
  }
}
