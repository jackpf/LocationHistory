package com.jackpf.locationhistory.server.repo

import com.jackpf.locationhistory.server.model.DeviceId.Type
import com.jackpf.locationhistory.server.model.{Location, StoredLocation}

import scala.concurrent.Future
import scala.util.Try

class SQLiteLocationRepo extends LocationRepo {
  override def storeDeviceLocation(
      id: Type,
      location: Location,
      timestamp: Long
  ): Future[Try[Unit]] = ???

  override def getForDevice(id: Type): Future[Vector[StoredLocation]] = ???

  override def deleteAll(): Future[Unit] = ???
}
