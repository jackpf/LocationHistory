package com.jackpf.locationhistory.server.repo

import com.jackpf.locationhistory.server.model.{DeviceId, Location}

import scala.concurrent.Future
import scala.util.Try

trait LocationRepo {
  def storeDeviceLocation(
      id: DeviceId.Type,
      location: Location
  ): Future[Try[Unit]]

  def getForDevice(id: DeviceId.Type): Future[Vector[Location]]

  def deleteAll(): Future[Unit]
}
