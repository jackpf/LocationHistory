package com.jackpf.locationhistory.server.repo

import com.jackpf.locationhistory.server.model.{DeviceId, Location, StoredLocation}
import com.jackpf.locationhistory.server.repo.LocationRepoExtensions.CheckDuplicateLocationFunc
import com.jackpf.locationhistory.server.testutil.{DefaultScope, DefaultSpecification, MockModels}
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq as eqTo
import org.mockito.Mockito.{mock, verify, when}
import org.mockito.internal.verification.Times
import org.specs2.concurrent.ExecutionEnv

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

class LocationRepoExtensionsTest(using ee: ExecutionEnv) extends DefaultSpecification {
  trait Context extends DefaultScope {
    val deviceId: DeviceId.Type = DeviceId("123")
    val newLocation: Location = MockModels.location()
    val newTimestamp: Long = 123L

    given ec: ExecutionContext = any[ExecutionContext]()
    val repository: LocationRepo = mock(classOf[LocationRepo])

    when(
      repository.storeDeviceLocationOrUpdatePrevious(
        any[DeviceId.Type](),
        any[Location](),
        any[Long](),
        any[CheckDuplicateLocationFunc]()
      )
    ).thenCallRealMethod()
  }

  "LocationRepoExtensions" should {
    "store a new location" >> in(new Context {
      when(repository.getForDevice(deviceId, limit = Some(1))).thenReturn(
        Future.successful(Vector.empty)
      )
      when(repository.storeDeviceLocation(deviceId, newLocation, newTimestamp))
        .thenReturn(Future.successful(Success(())))
    }) { context =>
      context.repository.storeDeviceLocationOrUpdatePrevious(
        context.deviceId,
        context.newLocation,
        context.newTimestamp,
        (_, _, _) => false
      ) must beSuccessfulTry.await

      verify(context.repository, Times(1))
        .storeDeviceLocation(context.deviceId, context.newLocation, context.newTimestamp)
      ok
    }

    "update a duplicate location" >> in(new Context {
      when(repository.getForDevice(deviceId, limit = Some(1))).thenReturn(
        Future.successful(Vector(MockModels.storedLocation()))
      )
      when(repository.update(eqTo(deviceId), eqTo(1L), any[StoredLocation => StoredLocation]()))
        .thenReturn(Future.successful(Success(())))
    }) { context =>
      context.repository.storeDeviceLocationOrUpdatePrevious(
        context.deviceId,
        context.newLocation,
        context.newTimestamp,
        (_, _, _) => true
      ) must beSuccessfulTry.await

      verify(context.repository, Times(1))
        .update(eqTo(context.deviceId), eqTo(1L), any[StoredLocation => StoredLocation]())
      ok
    }
  }
}
