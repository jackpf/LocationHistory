package com.jackpf.locationhistory.server.repo

import com.jackpf.locationhistory.server.model.{Device, StoredDevice}

import scala.concurrent.Future

trait DeviceRepo {
  def register(device: Device): Future[Unit]

  def getById(id: String): Future[Option[StoredDevice]]

  def get(device: Device): Future[Option[StoredDevice]]

  def getAll: Future[Seq[StoredDevice]]
}
