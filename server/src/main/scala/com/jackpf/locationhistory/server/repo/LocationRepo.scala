package com.jackpf.locationhistory.server.repo

import com.jackpf.locationhistory.server.model.{Location, StoredDevice}
import com.jackpf.locationhistory.server.util.GrpcResponse.GrpcTry

import scala.concurrent.Future

trait LocationRepo {
  def storeDeviceLocation(
      storedDevice: StoredDevice,
      location: Location
  ): Future[GrpcTry[Unit]]
}
