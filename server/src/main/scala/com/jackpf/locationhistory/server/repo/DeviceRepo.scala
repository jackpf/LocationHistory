package com.jackpf.locationhistory.server.repo

import com.jackpf.locationhistory.server.model.{Device, DeviceId, StoredDevice}

import scala.concurrent.Future
import scala.util.Try

trait DeviceRepo {
  def register(device: Device): Future[Try[Unit]]

  def update(storedDevice: StoredDevice): Future[Try[Unit]]

  def get(id: DeviceId.Type): Future[Option[StoredDevice]]

  def getAll: Future[Seq[StoredDevice]]

  def delete(id: DeviceId.Type): Future[Try[Unit]]

  def deleteAll(): Future[Unit]
}
