package com.jackpf.locationhistory.server.repo

import com.jackpf.locationhistory.server.model.{Location, StoredDevice}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.util.{Success, Try}

class InMemoryLocationRepo extends LocationRepo {
  private val storedLocations: mutable.ArrayBuffer[(StoredDevice, Location)] =
    ArrayBuffer().empty

  override def storeDeviceLocation(
      storedDevice: StoredDevice,
      location: Location
  ): Future[Try[Unit]] =
    Future.successful {
      storedLocations += ((storedDevice, location))
      Success[Unit](())
    }
}
