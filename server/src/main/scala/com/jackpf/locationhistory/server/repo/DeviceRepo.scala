package com.jackpf.locationhistory.server.repo

import com.jackpf.locationhistory.server.model.{Device, DeviceId, StoredDevice}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

trait DeviceRepo {
  def init(): Future[Unit] = Future.successful(())

  def register(device: Device): Future[Try[Unit]]

  def update(
      id: DeviceId.Type,
      updateAction: StoredDevice => StoredDevice
  ): Future[Try[Unit]]

  def get(id: DeviceId.Type): Future[Option[StoredDevice]]

  def getAll: Future[Seq[StoredDevice]]

  def delete(id: DeviceId.Type): Future[Try[Unit]]

  def deleteAll(): Future[Unit]

  /* Helpers */

  def getWithStatus(
      id: DeviceId.Type,
      status: StoredDevice.DeviceStatus
  )(using ec: ExecutionContext): Future[Option[StoredDevice]] =
    get(id).map {
      case Some(foundDevice) if foundDevice.status == status => Some(foundDevice)
      case _                                                 => None
    }

  def getPendingDevice(
      id: DeviceId.Type
  )(using ec: ExecutionContext): Future[Option[StoredDevice]] =
    getWithStatus(id, StoredDevice.DeviceStatus.Pending)

  def getRegisteredDevice(
      id: DeviceId.Type
  )(using ec: ExecutionContext): Future[Option[StoredDevice]] =
    getWithStatus(id, StoredDevice.DeviceStatus.Registered)
}
