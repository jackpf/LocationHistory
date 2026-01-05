package com.jackpf.locationhistory.server.repo

import com.jackpf.locationhistory.server.model.{DeviceId, Location, StoredLocation}
import com.jackpf.locationhistory.server.testutil.{DefaultScope, DefaultSpecification, GrpcMatchers}
import org.specs2.concurrent.ExecutionEnv

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.Try

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

    lazy val result: Future[Try[Unit]] = Future
      .sequence(locations.map { case (d, l, t) =>
        locationRepo.storeDeviceLocation(d, l, t)
      })
      .map(s => Try(s.map(_.get)))
  }

  "Location repo" should {
    "store a device location" >> in(new StoredLocationContext {}) { context =>
      context.result must beSuccessfulTry.await
    }

    "get locations by device" >> in(new StoredLocationContext {}) { context =>
      context.result must beSuccessfulTry.await

      context.locationRepo
        .getForDevice(DeviceId("123"), limit = None) must beEqualTo(
        Seq(StoredLocation(context.locations.head._2, context.locations.head._3))
      ).await
    }

    "get locations by device with limit" >> in(new StoredLocationContext {
      override lazy val locations: Seq[(DeviceId.Type, Location, Long)] = Seq(
        (DeviceId("123"), Location(lat = 0.1, lon = 0.2, accuracy = 0.3), 123L),
        (DeviceId("123"), Location(lat = 0.2, lon = 0.3, accuracy = 0.4), 456L)
      )
    }) { context =>
      context.result must beSuccessfulTry.await

      context.locationRepo
        .getForDevice(DeviceId("123"), limit = Some(1)) must beEqualTo(
        Seq(StoredLocation(context.locations.last._2, context.locations.last._3))
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
        Seq(StoredLocation(context.locations.head._2, context.locations.head._3))
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
