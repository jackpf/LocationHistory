package com.jackpf.locationhistory.server.repo

import com.jackpf.locationhistory.server.errors.ApplicationErrors.{
  DeviceNotFoundException,
  InvalidDeviceStatus
}
import com.jackpf.locationhistory.server.model.DeviceId.Type
import com.jackpf.locationhistory.server.model.{Device, DeviceId, StoredDevice}
import com.jackpf.locationhistory.server.testutil.{DefaultScope, DefaultSpecification, MockModels}
import org.specs2.concurrent.ExecutionEnv

import scala.concurrent.Future
import scala.util.Try

class DeviceRepoExtensionsTest(using ee: ExecutionEnv) extends DefaultSpecification {
  trait StubDeviceRepo extends DeviceRepo {
    override def register(device: Device): Future[Try[Unit]] = ???
    override def update(id: Type, updateAction: StoredDevice => StoredDevice): Future[Try[Unit]] =
      ???
    override def getAll: Future[Seq[StoredDevice]] = ???
    override def delete(id: Type): Future[Try[Unit]] = ???
    override def deleteAll(): Future[Unit] = ???
  }

  trait Context extends DefaultScope {
    val activeDevice: StoredDevice =
      MockModels.storedDevice(status = StoredDevice.DeviceStatus.Registered)
    val pendingDevice: StoredDevice =
      MockModels.storedDevice(status = StoredDevice.DeviceStatus.Pending)

    val repository: StubDeviceRepo
  }

  "DeviceRepoExtensions" should {
    "return success when device exists and status matches" >> in(new Context {
      override val repository: StubDeviceRepo =
        (_: DeviceId.Type) => Future.successful(Some(activeDevice))
    }) { context =>
      context.repository
        .getWithStatus(
          DeviceId("123"),
          StoredDevice.DeviceStatus.Registered
        )
        .map(_.get) must beEqualTo(
        context.activeDevice
      ).await
    }

    "return failure when status mismatch" >> in(new Context {
      override val repository: StubDeviceRepo =
        (_: DeviceId.Type) => Future.successful(Some(pendingDevice))
    }) { context =>
      context.repository
        .getWithStatus(
          DeviceId("123"),
          StoredDevice.DeviceStatus.Registered
        )
        .map(_.get) must throwA[InvalidDeviceStatus].await
    }

    "return failure when device not found" >> in(new Context {
      override val repository: StubDeviceRepo =
        (_: DeviceId.Type) => Future.successful(None)
    }) { context =>
      context.repository
        .getWithStatus(
          DeviceId("123"),
          StoredDevice.DeviceStatus.Registered
        )
        .map(_.get) must throwA[DeviceNotFoundException].await
    }
  }
}
