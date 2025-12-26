package com.jackpf.locationhistory.server.repo

import com.jackpf.locationhistory.server.model.DeviceId.Type
import com.jackpf.locationhistory.server.model.{DeviceId, Location}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.util.{Success, Try}

class InMemoryLocationRepo extends LocationRepo {
  private val storedLocations: mutable.ArrayBuffer[(DeviceId.Type, Location)] =
    ArrayBuffer().empty

  override def storeDeviceLocation(
      id: DeviceId.Type,
      location: Location
  ): Future[Try[Unit]] =
    Future.successful {
      storedLocations += ((id, location))
      Success(())
    }

  override def getForDevice(id: Type): Future[Seq[Location]] =
    Future.successful {
      // TODO Probably need a better way to store locations per device
      storedLocations.filter(_._1 == id).map(_._2).toSeq
    }

  override def deleteAll(): Future[Unit] = Future.successful {
    storedLocations.clear()
  }
}
