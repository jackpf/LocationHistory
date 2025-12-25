package com.jackpf.locationhistory.server.repo

import com.jackpf.locationhistory.server.model.{Location, StoredDevice}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

class InMemoryLocationRepo extends LocationRepo {
  private val storedLocations: mutable.ArrayBuffer[(StoredDevice, Location)] =
    ArrayBuffer().empty

  override def storeDeviceLocation(
      storedDevice: StoredDevice,
      location: Location
  ): Future[Unit] =
    Future.successful {
      storedLocations += ((storedDevice, location))
    }
}
