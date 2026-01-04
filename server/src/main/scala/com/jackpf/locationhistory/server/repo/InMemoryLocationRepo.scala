package com.jackpf.locationhistory.server.repo

import com.jackpf.locationhistory.server.model.{DeviceId, Location, StoredLocation}

import scala.collection.concurrent
import scala.concurrent.Future
import scala.util.{Success, Try}

object InMemoryLocationRepo {
  val DefaultMaxItemsPerDevice: Long = 1_000_000
}

class InMemoryLocationRepo(maxItemsPerDevice: Long = DefaultMaxItemsPerDevice)
    extends LocationRepo {
  private val storedLocations: concurrent.Map[DeviceId.Type, Vector[StoredLocation]] =
    concurrent.TrieMap.empty

  override def storeDeviceLocation(
      deviceId: DeviceId.Type,
      location: Location,
      timestamp: Long
  ): Future[Try[Unit]] = Future.successful {
    val storedLocation = StoredLocation.fromLocation(location, timestamp)

    storedLocations.updateWith(deviceId) {
      case Some(existingLocations) =>
        Some {
          val updated = existingLocations :+ storedLocation
          if (updated.size > maxItemsPerDevice) updated.drop(1) else updated
        }
      case None => Some(Vector(storedLocation))
    }
    Success(())
  }

  override def getForDevice(deviceId: DeviceId.Type): Future[Vector[StoredLocation]] =
    Future.successful {
      storedLocations.getOrElse(deviceId, Vector.empty)
    }

  override def deleteAll(): Future[Unit] = Future.successful {
    storedLocations.clear()
  }
}
