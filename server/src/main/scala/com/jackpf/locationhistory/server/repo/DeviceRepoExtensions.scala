package com.jackpf.locationhistory.server.repo

import com.jackpf.locationhistory.server.errors.ApplicationErrors.{
  DeviceNotFoundException,
  InvalidDeviceStatus
}
import com.jackpf.locationhistory.server.model.{DeviceId, StoredDevice}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

// TODO Make an extension
trait DeviceRepoExtensions { self: DeviceRepo =>
  def getWithStatus(
      id: DeviceId.Type,
      status: StoredDevice.DeviceStatus
  )(using ec: ExecutionContext): Future[Try[StoredDevice]] =
    get(id).map {
      case Some(foundDevice) if foundDevice.status == status => Success(foundDevice)
      case Some(foundDevice) => Failure(InvalidDeviceStatus(id, status, foundDevice.status))
      case _                 => Failure(DeviceNotFoundException(id))
    }

  def getPendingDevice(
      id: DeviceId.Type
  )(using ec: ExecutionContext): Future[Try[StoredDevice]] =
    getWithStatus(id, StoredDevice.DeviceStatus.Pending)

  def getRegisteredDevice(
      id: DeviceId.Type
  )(using ec: ExecutionContext): Future[Try[StoredDevice]] =
    getWithStatus(id, StoredDevice.DeviceStatus.Registered)
}
