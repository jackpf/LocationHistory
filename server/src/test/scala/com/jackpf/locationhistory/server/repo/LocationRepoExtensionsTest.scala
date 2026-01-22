package com.jackpf.locationhistory.server.repo

import com.jackpf.locationhistory.server.model.{DeviceId, Location, StoredLocation}
import com.jackpf.locationhistory.server.repo.LocationRepoExtensions.CheckDuplicateLocationFunc
import com.jackpf.locationhistory.server.testutil.{DefaultScope, DefaultSpecification, MockModels}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{mock, verify, when}
import org.mockito.internal.verification.Times
import org.specs2.concurrent.ExecutionEnv

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Try}

class LocationRepoExtensionsTest(using ee: ExecutionEnv) extends DefaultSpecification {
  trait Context extends DefaultScope {
    val deviceId: DeviceId.Type = DeviceId("123")
    val newLocation: Location = MockModels.location()
    val newTimestamp: Long = 123L
    val isDuplicate: Boolean

    val repository: LocationRepo = mock(classOf[LocationRepo])

    when(
      repository.storeDeviceLocationOrUpdatePrevious(
        any[DeviceId.Type](),
        any[Location](),
        any[Long](),
        any[CheckDuplicateLocationFunc]()
      )(using any[ExecutionContext]())
    ).thenCallRealMethod()

    lazy val result: Future[Try[Unit]] = repository.storeDeviceLocationOrUpdatePrevious(
      deviceId,
      newLocation,
      newTimestamp,
      (_, _, _) => isDuplicate
    )
  }

  trait StoreNewLocationContext extends Context {
    override val isDuplicate: Boolean = false

    when(repository.getForDevice(deviceId, limit = Some(1))).thenReturn(
      Future.successful(Vector.empty)
    )
    when(repository.storeDeviceLocation(deviceId, newLocation, newTimestamp))
      .thenReturn(Future.successful(Success(())))
  }

  trait UpdatePreviousLocationContext extends Context {
    lazy val storedLocation: StoredLocation = MockModels.storedLocation()
    override val isDuplicate: Boolean = true

    val updateCaptor: ArgumentCaptor[StoredLocation => StoredLocation] =
      ArgumentCaptor.forClass(classOf[StoredLocation => StoredLocation])

    when(repository.getForDevice(deviceId, limit = Some(1))).thenReturn(
      Future.successful(Vector(storedLocation))
    )
    when(repository.update(eqTo(deviceId), eqTo(1L), updateCaptor.capture()))
      .thenReturn(Future.successful(Success(())))
  }

  "LocationRepoExtensions" should {
    "store a new location" >> in(new StoreNewLocationContext {}) { context =>
      context.result must beSuccessfulTry.await

      verify(context.repository, Times(1))
        .storeDeviceLocation(context.deviceId, context.newLocation, context.newTimestamp)
      ok
    }

    "update a duplicate location" >> in(new UpdatePreviousLocationContext {}) { context =>
      context.result must beSuccessfulTry.await

      verify(context.repository, Times(1))
        .update(eqTo(context.deviceId), eqTo(1L), any[StoredLocation => StoredLocation]())
      ok
    }
  }
}
