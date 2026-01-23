package com.jackpf.locationhistory.server.repo

import com.jackpf.locationhistory.server.model.{DeviceId, Location, StoredLocation}

import scala.concurrent.Future
import scala.util.Try

trait LocationRepo extends LocationRepoExtensions {
  def init(): Future[Unit] = Future.successful(())

  def storeDeviceLocation(
      deviceId: DeviceId.Type,
      location: Location,
      startTimestamp: Long,
      endTimestamp: Long,
      count: Long
  ): Future[Try[Unit]]

  def getForDevice(deviceId: DeviceId.Type, limit: Option[Int]): Future[Vector[StoredLocation]]

  def update(
      deviceId: DeviceId.Type,
      id: Long,
      updateAction: StoredLocation => StoredLocation
  ): Future[Try[Unit]]

  def deleteForDevice(deviceId: DeviceId.Type): Future[Unit]

  def deleteAll(): Future[Unit]
}
