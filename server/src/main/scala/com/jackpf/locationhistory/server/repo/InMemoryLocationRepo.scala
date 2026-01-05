package com.jackpf.locationhistory.server.repo

import com.jackpf.locationhistory.server.model.DeviceId.Type
import com.jackpf.locationhistory.server.model.{DeviceId, Location, StoredLocation}

import java.util.concurrent.atomic.AtomicLong
import scala.collection.concurrent
import scala.concurrent.Future
import scala.util.{Success, Try}

object InMemoryLocationRepo {
  val DefaultMaxItemsPerDevice: Long = 1_000_000
}

class InMemoryLocationRepo(maxItemsPerDevice: Long = DefaultMaxItemsPerDevice)
    extends LocationRepo {
  private val incrementalId: AtomicLong = new AtomicLong(1)
  private val storedLocations: concurrent.Map[DeviceId.Type, Vector[StoredLocation]] =
    concurrent.TrieMap.empty

  private def generateId(): Long =
    incrementalId.getAndIncrement()

  override def storeDeviceLocation(
      deviceId: DeviceId.Type,
      location: Location,
      timestamp: Long
  ): Future[Try[Unit]] = Future.successful {
    val storedLocation =
      StoredLocation.fromLocation(location, id = generateId(), timestamp = timestamp)

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

  override def getForDevice(
      deviceId: DeviceId.Type,
      limit: Option[Int]
  ): Future[Vector[StoredLocation]] =
    Future.successful {
      val v = storedLocations.getOrElse(deviceId, Vector.empty)

      limit match {
        case Some(l) =>
          v.takeRight(l) // Assuming already timestamp ordered to avoid large .sortBy()
        case None => v
      }
    }

  override def deleteForDevice(deviceId: Type): Future[Unit] = Future.successful {
    storedLocations.remove(deviceId)
    ()
  }

  override def deleteAll(): Future[Unit] = Future.successful {
    storedLocations.clear()
  }
}
