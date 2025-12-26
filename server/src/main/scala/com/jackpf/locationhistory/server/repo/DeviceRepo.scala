package com.jackpf.locationhistory.server.repo

import com.jackpf.locationhistory.server.model.{Device, DeviceId, StoredDevice}
import com.jackpf.locationhistory.server.util.GrpcResponse.GrpcTry

import scala.concurrent.Future

trait DeviceRepo {
  def register(device: Device): Future[GrpcTry[Unit]]

  def get(id: DeviceId.Type): Future[Option[StoredDevice]]

  def getAll: Future[Seq[StoredDevice]]
}
