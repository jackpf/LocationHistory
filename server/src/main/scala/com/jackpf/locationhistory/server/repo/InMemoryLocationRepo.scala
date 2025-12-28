package com.jackpf.locationhistory.server.repo

import com.jackpf.locationhistory.server.model.{DeviceId, Location}

import scala.collection.concurrent
import scala.concurrent.Future
import scala.util.{Success, Try}

class InMemoryLocationRepo extends LocationRepo {
  private val storedLocations: concurrent.Map[DeviceId.Type, Vector[Location]] =
    concurrent.TrieMap.empty

  override def storeDeviceLocation(
      id: DeviceId.Type,
      location: Location
  ): Future[Try[Unit]] = Future.successful {
    storedLocations.updateWith(id) {
      case Some(existingLocations) => Some(existingLocations :+ location)
      case None                    => Some(Vector(location))
    }
    Success(())
  }

  override def getForDevice(id: DeviceId.Type): Future[Vector[Location]] =
    Future.successful {
      storedLocations.getOrElse(id, Vector.empty)
    }

  override def deleteAll(): Future[Unit] = Future.successful {
    storedLocations.clear()
  }
}
