package com.jackpf.locationhistory.server.repo

import com.jackpf.locationhistory.server.model.{DeviceId, Location, StoredLocation}

import scala.concurrent.Future
import scala.util.Try

trait LocationRepo {
  def init(): Future[Unit] = Future.successful(())

  def storeDeviceLocation(
      deviceId: DeviceId.Type,
      location: Location,
      timestamp: Long
  ): Future[Try[Unit]]

  def getForDevice(deviceId: DeviceId.Type): Future[Vector[StoredLocation]]

  def deleteForDevice(deviceId: DeviceId.Type): Future[Unit]

  def deleteAll(): Future[Unit]
}
