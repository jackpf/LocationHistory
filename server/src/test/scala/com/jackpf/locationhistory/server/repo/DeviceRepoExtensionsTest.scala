package com.jackpf.locationhistory.server.repo

import com.jackpf.locationhistory.server.errors.ApplicationErrors.{
  DeviceNotFoundException,
  InvalidDeviceStatus
}
import com.jackpf.locationhistory.server.model.{DeviceId, StoredDevice}
import com.jackpf.locationhistory.server.testutil.{DefaultScope, DefaultSpecification, MockModels}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{mock, when}
import org.specs2.concurrent.ExecutionEnv

import scala.concurrent.{ExecutionContext, Future}

class DeviceRepoExtensionsTest(using ee: ExecutionEnv) extends DefaultSpecification {
  trait Context extends DefaultScope {
    val activeDevice: StoredDevice =
      MockModels.storedDevice(status = StoredDevice.DeviceStatus.Registered)
    val pendingDevice: StoredDevice =
      MockModels.storedDevice(status = StoredDevice.DeviceStatus.Pending)

    given ec: ExecutionContext = any[ExecutionContext]()
    val repository: DeviceRepo = mock(classOf[DeviceRepo])
  }

  "DeviceRepoExtensions" should {
    "return success when device exists and status matches" >> in(new Context {
      when(repository.get(DeviceId("123"))).thenReturn(Future.successful(Some(activeDevice)))
      when(repository.getWithStatus(any[DeviceId.Type](), any[StoredDevice.DeviceStatus]()))
        .thenCallRealMethod()
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
      when(repository.get(DeviceId("123"))).thenReturn(Future.successful(Some(pendingDevice)))
      when(repository.getWithStatus(any[DeviceId.Type](), any[StoredDevice.DeviceStatus]()))
        .thenCallRealMethod()
    }) { context =>
      context.repository
        .getWithStatus(
          DeviceId("123"),
          StoredDevice.DeviceStatus.Registered
        )
        .map(_.get) must throwA[InvalidDeviceStatus].await
    }

    "return failure when device not found" >> in(new Context {
      when(repository.get(DeviceId("123"))).thenReturn(Future.successful(None))
      when(repository.getWithStatus(any[DeviceId.Type](), any[StoredDevice.DeviceStatus]()))
        .thenCallRealMethod()
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
