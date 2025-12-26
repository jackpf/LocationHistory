package com.jackpf.locationhistory.server.repo

import com.jackpf.locationhistory.server.model.{Location, StoredDevice}

import scala.concurrent.Future
import scala.util.Try

trait LocationRepo {
  def storeDeviceLocation(
      storedDevice: StoredDevice,
      location: Location
  ): Future[Try[Unit]]
}
