package com.jackpf.locationhistory.server.repo

import com.jackpf.locationhistory.server.model.{DeviceId, Location, StoredLocation}

import scala.concurrent.Future
import scala.util.Try

trait LocationRepo {
  def storeDeviceLocation(
      id: DeviceId.Type,
      location: Location,
      timestamp: Long
  ): Future[Try[Unit]]

  def getForDevice(id: DeviceId.Type): Future[Vector[StoredLocation]]

  def deleteAll(): Future[Unit]
}
