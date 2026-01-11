package com.jackpf.locationhistory.server.repo

import com.jackpf.locationhistory.server.model.{DeviceId, Location, StoredLocation}
import com.jackpf.locationhistory.server.testutil.{DefaultScope, DefaultSpecification, GrpcMatchers}
import org.specs2.concurrent.ExecutionEnv

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Success, Try}

abstract class LocationRepoTest(implicit ee: ExecutionEnv)
    extends DefaultSpecification
    with GrpcMatchers {
  def createLocationRepo: LocationRepo

  trait Context extends DefaultScope {
    val locationRepo: LocationRepo = createLocationRepo
    Await.result(locationRepo.init(), Duration.Inf)
  }

  trait StoredLocationContext extends Context {
    lazy val locations: Seq[(DeviceId.Type, Location, Long)] = Seq(
      (DeviceId("123"), Location(lat = 0.1, lon = 0.2, accuracy = 0.3), 123L)
    )

    lazy val result: Future[Try[Unit]] = {
      // Guarantee insertion order
      locations.foldLeft(Future.successful(Try(()))) { (acc, item) =>
        acc.flatMap {
          case Success(_) =>
            val (d, l, t) = item
            locationRepo.storeDeviceLocation(d, l, t)

          case failure =>
            Future.successful(failure)
        }
      }
    }
  }

  "Location repo" should {
    "store a device location" >> in(new StoredLocationContext {}) { context =>
      context.result must beSuccessfulTry.await
    }

    "get locations by device" >> in(new StoredLocationContext {}) { context =>
      context.result must beSuccessfulTry.await

      context.locationRepo
        .getForDevice(DeviceId("123"), limit = None) must beEqualTo(
        Seq(StoredLocation(1L, context.locations.head._2, context.locations.head._3))
      ).await
    }

    "get locations by device with limit" >> in(new StoredLocationContext {
      override lazy val locations: Seq[(DeviceId.Type, Location, Long)] = Seq(
        (DeviceId("123"), Location(lat = 0.1, lon = 0.2, accuracy = 0.3), 123L),
        (DeviceId("123"), Location(lat = 0.2, lon = 0.3, accuracy = 0.4), 456L),
        (DeviceId("123"), Location(lat = 0.5, lon = 0.6, accuracy = 0.4), 789L)
      )
    }) { context =>
      context.result must beSuccessfulTry.await

      context.locationRepo
        .getForDevice(DeviceId("123"), limit = Some(2)) must beEqualTo(
        Seq(
          StoredLocation(2L, context.locations(1)._2, context.locations(1)._3),
          StoredLocation(3L, context.locations(2)._2, context.locations(2)._3)
        )
      ).await
    }

    "get empty locations by device" >> in(new StoredLocationContext {}) { context =>
      context.result must beSuccessfulTry.await

      context.locationRepo
        .getForDevice(DeviceId("non-existing"), limit = None) must beEmpty[
        Seq[StoredLocation]
      ].await
    }

    "delete locations for a device" >> in(new StoredLocationContext {
      override lazy val locations: Seq[(DeviceId.Type, Location, Long)] = Seq(
        (DeviceId("123"), Location(lat = 0.1, lon = 0.2, accuracy = 0.3), 123L),
        (DeviceId("456"), Location(lat = 0.3, lon = 0.4, accuracy = 0.3), 123L)
      )
    }) { context =>
      context.result must beSuccessfulTry.await

      context.locationRepo.deleteForDevice(DeviceId("456")) must beEqualTo(()).await

      context.locationRepo
        .getForDevice(DeviceId("123"), limit = None) must beEqualTo(
        Seq(StoredLocation(1L, context.locations.head._2, context.locations.head._3))
      ).await
      context.locationRepo
        .getForDevice(DeviceId("456"), limit = None) must beEmpty[Seq[StoredLocation]].await
    }

    "delete all locations" >> in(new StoredLocationContext {
      override lazy val locations: Seq[(DeviceId.Type, Location, Long)] = Seq(
        (DeviceId("123"), Location(lat = 0.1, lon = 0.2, accuracy = 0.3), 123L),
        (DeviceId("456"), Location(lat = 0.3, lon = 0.4, accuracy = 0.3), 123L)
      )
    }) { context =>
      context.result must beSuccessfulTry.await

      context.locationRepo.deleteAll() must beEqualTo(()).await

      context.locationRepo
        .getForDevice(DeviceId("123"), limit = None) must beEmpty[Seq[StoredLocation]].await
      context.locationRepo
        .getForDevice(DeviceId("456"), limit = None) must beEmpty[Seq[StoredLocation]].await
    }
  }
}
